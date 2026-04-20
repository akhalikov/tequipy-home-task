package com.tequipy.equipment;

import com.tequipy.IntegrationTestBase;
import com.tequipy.equipment.controller.request.EquipmentRegisterRequest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.tequipy.equipment.domain.EquipmentState.AVAILABLE;
import static com.tequipy.equipment.domain.EquipmentState.RESERVED;
import static com.tequipy.equipment.domain.EquipmentState.RETIRED;
import static com.tequipy.equipment.domain.EquipmentType.KEYBOARD;
import static com.tequipy.equipment.domain.EquipmentType.MONITOR;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EquipmentControllerTest extends IntegrationTestBase {

    @Nested
    class RegisterEquipmentTests {

        @Test
        void registers_equipment() throws Exception {
            // given
            var request = EquipmentRegisterRequest.builder()
                .type(MONITOR)
                .brand("Dell")
                .model("U2723D")
                .conditionScore(new BigDecimal("0.95"))
                .purchaseDate(LocalDate.of(2023, 1, 15))
                .build();

            // when / then
            mockMvc.perform(post("/equipments")
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.type").value("MONITOR"))
                .andExpect(jsonPath("$.brand").value("Dell"))
                .andExpect(jsonPath("$.model").value("U2723D"))
                .andExpect(jsonPath("$.state").value("AVAILABLE"))
                .andExpect(jsonPath("$.conditionScore").value(0.95))
                .andExpect(jsonPath("$.purchaseDate").value("2023-01-15"));
        }

        @Test
        void rejects_null_type() throws Exception {
            // given
            var request = EquipmentRegisterRequest.builder()
                .brand("Dell")
                .model("U2723D")
                .conditionScore(new BigDecimal("0.90"))
                .purchaseDate(LocalDate.of(2023, 1, 15))
                .build();

            // when / then
            mockMvc.perform(post("/equipments")
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        void rejects_blank_brand() throws Exception {
            // given
            var request = EquipmentRegisterRequest.builder()
                .type(MONITOR)
                .brand("")
                .model("U2723D")
                .conditionScore(new BigDecimal("0.90"))
                .purchaseDate(LocalDate.of(2023, 1, 15))
                .build();

            // when / then
            mockMvc.perform(post("/equipments")
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        void rejects_blank_model() throws Exception {
            // given
            var request = EquipmentRegisterRequest.builder()
                .type(MONITOR)
                .brand("Dell")
                .model("")
                .conditionScore(new BigDecimal("0.90"))
                .purchaseDate(LocalDate.of(2023, 1, 15))
                .build();

            // when / then
            mockMvc.perform(post("/equipments")
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        void rejects_condition_score_below_minimum() throws Exception {
            // given
            var request = EquipmentRegisterRequest.builder()
                .type(MONITOR)
                .brand("Dell")
                .model("U2723D")
                .conditionScore(new BigDecimal("-0.01"))
                .purchaseDate(LocalDate.of(2023, 1, 15))
                .build();

            // when / then
            mockMvc.perform(post("/equipments")
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        void rejects_condition_score_above_maximum() throws Exception {
            // given
            var request = EquipmentRegisterRequest.builder()
                .type(MONITOR)
                .brand("Dell")
                .model("U2723D")
                .conditionScore(new BigDecimal("1.01"))
                .purchaseDate(LocalDate.of(2023, 1, 15))
                .build();

            // when / then
            mockMvc.perform(post("/equipments")
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        void rejects_future_purchase_date() throws Exception {
            // given
            var request = EquipmentRegisterRequest.builder()
                .type(MONITOR)
                .brand("Dell")
                .model("U2723D")
                .conditionScore(new BigDecimal("0.90"))
                .purchaseDate(LocalDate.now().plusDays(1))
                .build();

            // when / then
            mockMvc.perform(post("/equipments")
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class RetireEquipmentTests {

        @Test
        void retires_equipment() throws Exception {
            // given
            var id = registerEquipment(MONITOR, "Dell", "U2723D", "0.90");

            // when / then
            mockMvc.perform(post("/equipments/{id}/retire", id)
                    .contentType(APPLICATION_JSON)
                    .content("""
                        {"reason": "damaged"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.state").value("RETIRED"))
                .andExpect(jsonPath("$.retireReason").value("damaged"));
        }

        @Test
        void retired_equipment_is_visible_in_list_with_state_filter() throws Exception {
            // given
            var id = registerEquipment(MONITOR, "Dell", "U2723D", "0.90");
            retireEquipment(id, "end of life");

            // when / then
            mockMvc.perform(get("/equipments").param("state", RETIRED.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].state").value("RETIRED"))
                .andExpect(jsonPath("$[0].retireReason").value("end of life"));
        }

        @Test
        void returns_404_when_equipment_not_found() throws Exception {
            // when / then
            mockMvc.perform(post("/equipments/{id}/retire", randomUUID())
                    .contentType(APPLICATION_JSON)
                    .content("""
                        {"reason": "damaged"}
                        """))
                .andExpect(status().isNotFound());
        }

        @Test
        void rejects_blank_reason() throws Exception {
            // given
            var id = registerEquipment(MONITOR, "Dell", "U2723D", "0.90");

            // when / then
            mockMvc.perform(post("/equipments/{id}/retire", id)
                    .contentType(APPLICATION_JSON)
                    .content("""
                        {"reason": ""}
                        """))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class ListEquipmentsTests {

        @Test
        void returns_empty_list_when_no_equipment_registered() throws Exception {
            mockMvc.perform(get("/equipments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        void returns_all_registered_equipment() throws Exception {
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
        void filters_by_state() throws Exception {
            // given
            registerEquipment(MONITOR, "Dell", "U2723D", "0.90");
            registerEquipment(KEYBOARD, "Apple", "Magic Keyboard", "0.80");

            // when / then
            mockMvc.perform(get("/equipments").param("state", AVAILABLE.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].state").value(everyItem(is("AVAILABLE"))));
        }

        @Test
        void filter_by_state_returns_empty_when_no_match() throws Exception {
            // given — freshly registered equipment is AVAILABLE, not RESERVED
            registerEquipment(MONITOR, "Dell", "U2723D", "0.90");

            // when / then
            mockMvc.perform(get("/equipments").param("state", RESERVED.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        }
    }
}
