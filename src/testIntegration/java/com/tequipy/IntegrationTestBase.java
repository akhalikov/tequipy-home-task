package com.tequipy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tequipy.allocation.controller.request.AllocationRequest;
import com.tequipy.allocation.controller.request.PolicyItemRequest;
import com.tequipy.equipment.controller.request.EquipmentRegisterRequest;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        cleanUpDatabase();
    }

    protected UUID createAllocationRequest(String employeeId, PolicyItemRequest... policyItems) throws Exception {
        var request = AllocationRequest.builder()
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

    protected void retireEquipment(UUID id, String reason) throws Exception {
        mockMvc.perform(post("/equipments/{id}/retire", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("reason", reason))))
            .andExpect(status().isOk());
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
