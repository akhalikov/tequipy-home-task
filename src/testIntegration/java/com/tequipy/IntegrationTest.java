package com.tequipy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tequipy.allocation.controller.PolicyItemRequest;
import com.tequipy.allocation.repository.AllocationRequestRepository;
import com.tequipy.equipment.controller.EquipmentRegisterRequest;
import com.tequipy.equipment.domain.EquipmentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.tequipy.allocation.controller.AllocateEquipmentRequest.allocateEquipmentRequestBuilder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public abstract class IntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected AllocationRequestRepository allocationRequestRepository;

    protected UUID createAllocationRequest(String employeeId, PolicyItemRequest... policyItems) throws Exception {
        var request = allocateEquipmentRequestBuilder()
            .employeeId(employeeId)
            .policy(List.of(policyItems))
            .build();

        var result = mockMvc.perform(post("/allocations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn();

        return idFrom(result);
    }

    protected UUID registerEquipment(EquipmentType type, String brand, String model, String score) throws Exception {
        var request = EquipmentRegisterRequest.builder()
            .type(type)
            .brand(brand)
            .model(model)
            .conditionScore(new BigDecimal(score))
            .purchaseDate(LocalDate.of(2022, 6, 1))
            .build();

        var result = mockMvc.perform(post("/equipments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn();

        return idFrom(result);
    }

    protected void allocateEquipment(UUID requestId) {
        final var updated = allocationRequestRepository.getBy(requestId).allocate();
        allocationRequestRepository.save(updated);
    }

    protected UUID idFrom(MvcResult result) throws Exception {
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }
}
