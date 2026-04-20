package com.tequipy.equipment;

import com.tequipy.IntegrationTestBase;
import com.tequipy.equipment.controller.EquipmentRegisterRequest;
import com.tequipy.equipment.domain.EquipmentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.tequipy.equipment.domain.EquipmentType.KEYBOARD;
import static com.tequipy.equipment.domain.EquipmentType.MONITOR;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EquipmentControllerTest extends IntegrationTestBase {

    @Test
    void registers_equipment() throws Exception {
        // given
        var request = EquipmentRegisterRequest.builder()
            .type(MONITOR)
            .brand("Sony")
            .model("U2723D")
            .conditionScore(new BigDecimal("0.95"))
            .purchaseDate(LocalDate.of(2023, 1, 15))
            .build();

        // when / then
        mockMvc.perform(post("/equipments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.type").value("MONITOR"))
            .andExpect(jsonPath("$.brand").value("Sony"))
            .andExpect(jsonPath("$.state").value("AVAILABLE"))
            .andExpect(jsonPath("$.conditionScore").value(0.95));
    }

    @Test
    void lists_all_equipment() throws Exception {
        // given
        var id1 = registerEquipment(MONITOR, "Dell", "U2723D", "0.90");
        var id2 = registerEquipment(KEYBOARD, "Apple", "Magic Keyboard", "0.80");

        // when / then
        mockMvc.perform(get("/equipments"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[*].id").value(containsInAnyOrder(id1.toString(), id2.toString())));
    }

    @Test
    void filters_equipment_by_state() throws Exception {
        // given
        registerEquipment(EquipmentType.KEYBOARD, "Apple", "Magic Keyboard", "0.80");

        // when / then
        mockMvc.perform(get("/equipments").param("state", "AVAILABLE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[*].state").value(everyItem(is("AVAILABLE"))));
    }

    @Test
    void rejects_invalid_condition_score() throws Exception {
        // given
        var invalidRequest = """
            {"type":"MONITOR","brand":"Dell","model":"X","conditionScore":1.5,"purchaseDate":"2023-01-01"}
            """;

        // when / then
        mockMvc.perform(post("/equipments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
            .andExpect(status().isBadRequest());
    }

    @Test
    void rejects_blank_brand() throws Exception {
        // given
        var invalidRequest = """
            {"type":"MONITOR","brand":"","model":"X","conditionScore":0.9,"purchaseDate":"2023-01-01"}
            """;

        // when / then
        mockMvc.perform(post("/equipments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
            .andExpect(status().isBadRequest());
    }
}
