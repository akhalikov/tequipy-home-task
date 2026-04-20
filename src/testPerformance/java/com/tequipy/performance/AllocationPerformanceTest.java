package com.tequipy.performance;

import com.tequipy.allocation.controller.request.AllocationRequest;
import com.tequipy.allocation.controller.AllocationResponse;
import com.tequipy.equipment.domain.EquipmentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static com.tequipy.allocation.controller.request.PolicyItemRequest.policyItemRequestBuilder;
import static com.tequipy.allocation.domain.AllocationState.PENDING;
import static com.tequipy.equipment.domain.EquipmentType.KEYBOARD;
import static com.tequipy.equipment.domain.EquipmentType.MAIN_COMPUTER;
import static com.tequipy.equipment.domain.EquipmentType.MONITOR;
import static com.tequipy.equipment.domain.EquipmentType.MOUSE;
import static java.util.Collections.sort;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
class AllocationPerformanceTest {

    private static final int EQUIPMENT_COUNT = 5_000;
    private static final int WARMUP_ITERATIONS = 20;
    private static final int MEASURE_ITERATIONS = 100;
    private static final String[] BRANDS = {"Apple", "Dell", "HP", "Lenovo", "Samsung", "LG", "Logitech"};

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void populateEquipment() {
        final Random random = new Random(42);
        final EquipmentType[] types = {MAIN_COMPUTER, MONITOR, MONITOR, KEYBOARD, MOUSE};
        jdbcTemplate.batchUpdate(
            "INSERT INTO equipment (id, type, brand, model, state, condition_score, purchase_date, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, 'AVAILABLE', ?, ?, NOW(), NOW())",
            generateEquipmentBatch(EQUIPMENT_COUNT, types, BRANDS, random)
        );
    }

    @AfterEach
    void cleanUp() {
        waitForNoPendingAllocations();
        jdbcTemplate.execute("DELETE FROM allocated_equipment");
        jdbcTemplate.execute("DELETE FROM allocation_requests");
        jdbcTemplate.execute("DELETE FROM equipment");
    }

    @Test
    void allocation_latency_typical_policy() {
        AllocationRequest request = typicalPolicy();
        runBenchmark("Typical policy (laptop + 2 monitors + keyboard)", request);
    }

    @Test
    void allocation_latency_single_item_policy() {
        AllocationRequest request = singleItemPolicy();
        runBenchmark("Single item policy (1 monitor)", request);
    }

    private void runBenchmark(String scenario, AllocationRequest request) {
        System.out.printf("%n=== %s ===%n", scenario);
        System.out.printf("Equipment pool: %,d | Warmup: %d | Samples: %d%n",
            EQUIPMENT_COUNT, WARMUP_ITERATIONS, MEASURE_ITERATIONS);

        // warmup — let JIT compile hot paths
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            UUID id = postAllocation(request);
            waitForTerminalState(id);
        }

        // measure
        List<Long> latenciesMs = new ArrayList<>(MEASURE_ITERATIONS);
        for (int i = 0; i < MEASURE_ITERATIONS; i++) {
            long start = System.nanoTime();
            UUID id = postAllocation(request);
            waitForTerminalState(id);
            latenciesMs.add((System.nanoTime() - start) / 1_000_000);
        }

        printResults(latenciesMs);
    }

    private UUID postAllocation(AllocationRequest request) {
        ResponseEntity<AllocationResponse> response = restTemplate.postForEntity(
            "/allocations", request, AllocationResponse.class);
        return response.getBody().id();
    }

    private void waitForNoPendingAllocations() {
        await()
            .atMost(15, SECONDS)
            .pollInterval(100, MILLISECONDS)
            .until(() -> {
                Integer pending = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM allocation_requests WHERE state = 'PENDING'", Integer.class);
                return pending != null && pending == 0;
            });
    }

    private void waitForTerminalState(UUID id) {
        await()
            .atMost(10, SECONDS)
            .pollInterval(5, MILLISECONDS)
            .until(() -> {
                final var response = restTemplate.getForObject("/allocations/{id}", AllocationResponse.class, id);
                return response.state() != PENDING;
            });
    }

    private static void printResults(List<Long> latenciesMs) {
        sort(latenciesMs);
        final long p50 = percentile(latenciesMs, 50);
        final long p95 = percentile(latenciesMs, 95);
        final long p99 = percentile(latenciesMs, 99);
        final long min = latenciesMs.get(0);
        final long max = latenciesMs.get(latenciesMs.size() - 1);
        final double mean = latenciesMs.stream().mapToLong(Long::longValue).average().orElse(0);
        System.out.printf("Results (ms): min=%d  mean=%.1f  p50=%d  p95=%d  p99=%d  max=%d%n",
            min, mean, p50, p95, p99, max);
    }

    private static long percentile(List<Long> sorted, int percentile) {
        int index = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(0, index));
    }

    private static AllocationRequest typicalPolicy() {
        return AllocationRequest.builder()
            .employeeId("perf-employee-" + randomUUID())
            .policy(List.of(
                policyItemRequestBuilder().equipmentType(MAIN_COMPUTER).build(),
                policyItemRequestBuilder().equipmentType(MONITOR).build(),
                policyItemRequestBuilder().equipmentType(MONITOR).build(),
                policyItemRequestBuilder().equipmentType(KEYBOARD).build()
            ))
            .build();
    }

    private static AllocationRequest singleItemPolicy() {
        return AllocationRequest.builder()
            .employeeId("perf-employee-" + randomUUID())
            .policy(List.of(policyItemRequestBuilder().equipmentType(MONITOR).build()))
            .build();
    }

    private static List<Object[]> generateEquipmentBatch(int count, EquipmentType[] types, String[] brands, Random random) {
        final List<Object[]> batch = new ArrayList<>(count);
        final LocalDate today = LocalDate.now();
        for (int i = 0; i < count; i++) {
            final var type = types[random.nextInt(types.length)];
            final var brand = brands[random.nextInt(brands.length)];
            final double conditionScore = 0.5 + random.nextDouble() * 0.5; // 0.50 – 1.00
            final var purchaseDate = today.minusDays(random.nextInt(365 * 5));
            batch.add(new Object[]{
                randomUUID().toString(),
                type.name(),
                brand,
                brand + " " + type.name() + " " + i,
                String.format("%.2f", conditionScore),
                purchaseDate
            });
        }
        return batch;
    }
}
