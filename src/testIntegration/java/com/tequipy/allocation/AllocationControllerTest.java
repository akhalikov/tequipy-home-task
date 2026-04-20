package com.tequipy.allocation;

import com.tequipy.IntegrationTestBase;
import com.tequipy.allocation.controller.request.AllocationRequest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.tequipy.TestData.policyItemRequest;
import static com.tequipy.TestData.randomEmployeeId;
import static com.tequipy.allocation.controller.request.PolicyItemRequest.policyItemRequestBuilder;
import static com.tequipy.allocation.domain.AllocationState.ALLOCATED;
import static com.tequipy.allocation.domain.AllocationState.FAILED;
import static com.tequipy.equipment.domain.EquipmentType.KEYBOARD;
import static com.tequipy.equipment.domain.EquipmentType.MONITOR;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AllocationControllerTest extends IntegrationTestBase {

    @Nested
    class CreateAllocationRequestTests {

        @Test
        void creates_allocation_request() throws Exception {
            // given
            var employeeId = randomEmployeeId();
            var request = AllocationRequest.builder()
                .employeeId(employeeId)
                .policy(List.of(policyItemRequestBuilder().equipmentType(MONITOR).build()))
                .build();

            // when / then
            mockMvc.perform(post("/allocations")
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.employeeId").value(employeeId))
                .andExpect(jsonPath("$.state").value("PENDING"));
        }

        @Test
        void rejects_allocation_with_empty_policy() throws Exception {
            // given
            var request = AllocationRequest.builder()
                .employeeId(randomEmployeeId())
                .build();

            // when / then
            mockMvc.perform(post("/allocations")
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        void rejects_allocation_with_blank_employee_id() throws Exception {
            // given
            var request = AllocationRequest.builder()
                .employeeId("")
                .build();

            // when / then
            mockMvc.perform(post("/allocations")
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class AllocationTests {

        @Test
        void allocates_equipment_that_meets_minimum_condition_score() throws Exception {
            // given - monitor exists and meets the required minimum condition score of 0.80
            registerEquipment(MONITOR, "Dell", "U2723D", "0.80");
            var policy = policyItemRequestBuilder()
                .equipmentType(MONITOR)
                .minConditionScore(new BigDecimal("0.80"))
                .build();

            // when
            var requestId = createAllocationRequest(randomEmployeeId(), policy);

            // then
            waitForAllocated(requestId);
        }

        @Test
        void fails_when_no_equipment_of_required_type_available() throws Exception {
            // given — only a keyboard exists, but the policy requires a monitor
            registerEquipment(KEYBOARD, "Apple", "Magic Keyboard", "0.90");

            // when
            var requestId = createAllocationRequest(randomEmployeeId(), policyItemRequest(MONITOR));

            // then
            waitForFailed(requestId);
        }

        @Test
        void fails_when_all_equipment_below_minimum_condition_score() throws Exception {
            // given — monitor exists but its score (0.50) is below the required minimum (0.80)
            registerEquipment(MONITOR, "Dell", "U2723D", "0.50");
            var policy = policyItemRequestBuilder()
                .equipmentType(MONITOR)
                .minConditionScore(new BigDecimal("0.80"))
                .build();

            // when
            var requestId = createAllocationRequest(randomEmployeeId(), policy);

            // then
            waitForFailed(requestId);
        }

        @Test
        void skips_equipment_below_minimum_and_selects_qualifying_one() throws Exception {
            // given — two monitors: one below threshold, one above; only the qualifying one must be picked
            registerEquipment(MONITOR, "Dell", "U2723D", "0.50");
            registerEquipment(MONITOR, "Dell", "U2723D", "0.90");
            var policy = policyItemRequestBuilder()
                .equipmentType(MONITOR)
                .minConditionScore(new BigDecimal("0.80"))
                .build();

            // when
            var requestId = createAllocationRequest(randomEmployeeId(), policy);

            // then — allocation succeeds because one monitor qualifies
            waitForAllocated(requestId);
        }

        @Test
        void fails_allocation_when_only_matching_equipment_is_retired() throws Exception {
            // given - a monitor exists but is retired, so it should not be allocated and the request should fail
            var equipmentId = registerEquipment(MONITOR, "Dell", "U2723D", "0.90");
            retireEquipment(equipmentId, "end of life");

            // when
            var requestId = createAllocationRequest(randomEmployeeId(), policyItemRequest(MONITOR));

            // then
            waitForFailed(requestId);
            mockMvc.perform(get("/allocations/{id}", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("FAILED"));
        }
    }

    @Nested
    class GetAllocationByIdTests {

        @Test
        void returns_allocation_by_id() throws Exception {
            // given
            var employeeId = randomEmployeeId();
            var policy = policyItemRequest(KEYBOARD);
            var requestId = createAllocationRequest(employeeId, policy);

            // when / then
            mockMvc.perform(get("/allocations/{id}", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(requestId.toString()))
                .andExpect(jsonPath("$.employeeId").value(employeeId));
        }

        @Test
        void returns_404_when_allocation_is_missing() throws Exception {
            // when / then
            mockMvc.perform(get("/allocations/{id}", randomUUID()))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    class ConfirmAllocationTests {

        @Test
        void confirms_allocation() throws Exception {
            // given
            registerEquipment(MONITOR, "Dell", "U2723D", "0.90");
            var employeeId = randomEmployeeId();
            var requestId = createAllocationRequest(employeeId, policyItemRequest(MONITOR));
            waitForAllocated(requestId);

            // when / then
            mockMvc.perform(post("/allocations/{id}/confirm", requestId)
                    .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(requestId.toString()))
                .andExpect(jsonPath("$.employeeId").value(employeeId))
                .andExpect(jsonPath("$.state").value("CONFIRMED"));
        }

        @Test
        void fails_request_when_equipment_is_not_allocated() throws Exception {
            // given
            var employeeId = randomEmployeeId();
            var requestId = createAllocationRequest(employeeId, policyItemRequest(MONITOR));
            waitForFailed(requestId);

            // when / then
            mockMvc.perform(post("/allocations/{id}/confirm", requestId)
                    .contentType(APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Can't confirm allocation in state: FAILED"));
        }
    }

    @Nested
    class CancelAllocationTests {

        @Test
        void cancels_allocation() throws Exception {
            // given
            registerEquipment(MONITOR, "Dell", "U2723D", "0.90");
            var employeeId = randomEmployeeId();
            var requestId = createAllocationRequest(employeeId, policyItemRequest(MONITOR));
            waitForAllocated(requestId);

            // when / then
            mockMvc.perform(post("/allocations/{id}/cancel", requestId)
                    .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.employeeId").value(employeeId))
                .andExpect(jsonPath("$.state").value("CANCELLED"));
        }

        @Test
        void fails_when_request_is_already_confirmed() throws Exception {
            // given
            var employeeId = randomEmployeeId();
            registerEquipment(MONITOR, "Dell", "U2723D", "0.80");
            var policy = policyItemRequestBuilder().equipmentType(MONITOR).build();

            var requestId = createAllocationRequest(employeeId, policy);
            waitForAllocated(requestId);

            // when - conforming the request
            mockMvc.perform(post("/allocations/{id}/confirm", requestId)
                    .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());

            // and / then
            mockMvc.perform(post("/allocations/{id}/cancel", requestId)
                    .contentType(APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Can't cancel allocation in state: CONFIRMED"));
        }
    }

    private void waitForAllocated(UUID requestId) {
        await()
            .atMost(5, SECONDS)
            .pollInterval(100, MILLISECONDS)
            .until(() -> {
                var response = mockMvc.perform(get("/allocations/{id}", requestId))
                    .andReturn().getResponse().getContentAsString();

                var state = objectMapper.readTree(response).get("state").asText();
                return state.equals(ALLOCATED.name());
            });
    }

    private void waitForFailed(UUID requestId) {
        await()
            .atMost(5, SECONDS)
            .pollInterval(100, MILLISECONDS)
            .until(() -> {
                var response = mockMvc.perform(get("/allocations/{id}", requestId))
                    .andReturn().getResponse().getContentAsString();

                var state = objectMapper.readTree(response).get("state").asText();
                return state.equals(FAILED.name());
            });
    }
}
