package com.tequipy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tequipy.allocation.controller.PolicyItemRequest;
import com.tequipy.allocation.repository.AllocationRequestRepository;
import com.tequipy.equipment.controller.EquipmentRegisterRequest;
import com.tequipy.equipment.domain.EquipmentType;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.tequipy.allocation.controller.AllocateEquipmentRequest.allocateEquipmentRequestBuilder;
import static java.util.Collections.emptyList;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class IntegrationTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected AllocationRequestRepository allocationRequestRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        cleanUpDatabase();
    }

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
        final var updated = allocationRequestRepository.getBy(requestId).allocate(emptyList());
        allocationRequestRepository.save(updated);
    }

    protected UUID idFrom(MvcResult result) throws Exception {
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    protected void cleanUpDatabase() {
        jdbcTemplate.execute("DELETE FROM allocated_equipment");
        jdbcTemplate.execute("DELETE FROM allocation_requests");
        jdbcTemplate.execute("DELETE FROM equipment");
    }
}
