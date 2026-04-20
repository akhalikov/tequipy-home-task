package com.tequipy.allocation;

import com.tequipy.IntegrationTestBase;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static com.tequipy.TestData.policyItemRequest;
import static com.tequipy.TestData.randomEmployeeId;
import static com.tequipy.allocation.controller.AllocateEquipmentRequest.allocateEquipmentRequestBuilder;
import static com.tequipy.allocation.controller.PolicyItemRequest.policyItemRequestBuilder;
import static com.tequipy.allocation.domain.AllocationState.ALLOCATED;
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
            var request = allocateEquipmentRequestBuilder()
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
            var request = allocateEquipmentRequestBuilder()
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
            var request = allocateEquipmentRequestBuilder()
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
}
