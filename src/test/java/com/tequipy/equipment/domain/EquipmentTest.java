package com.tequipy.equipment.domain;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static com.tequipy.TestData.equipment;
import static com.tequipy.equipment.domain.EquipmentState.ASSIGNED;
import static com.tequipy.equipment.domain.EquipmentState.AVAILABLE;
import static com.tequipy.equipment.domain.EquipmentState.RESERVED;
import static com.tequipy.equipment.domain.EquipmentState.RETIRED;
import static com.tequipy.equipment.domain.EquipmentType.MONITOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;

class EquipmentTest {

    @Nested
    class ReserveTests {

        @Test
        void reserves_available_equipment() {
            // given
            var equipment = equipment().type(MONITOR).state(AVAILABLE).build();

            // when
            var reserved = equipment.reserve();

            // then
            assertThat(reserved.getState()).isEqualTo(RESERVED);
        }

        @ParameterizedTest
        @EnumSource(value = EquipmentState.class, names = "AVAILABLE", mode = EXCLUDE)
        void throws_error_when_not_available(EquipmentState state) {
            // given
            var equipment = equipment().type(MONITOR).state(state).build();

            // when / then
            assertThatThrownBy(equipment::reserve)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only available equipment can be reserved, but is: " + state);
        }
    }

    @Nested
    class AssignTests {

        @Test
        void assigns_reserved_equipment() {
            // given
            var equipment = equipment().type(MONITOR).state(RESERVED).build();

            // when
            var assigned = equipment.assign();

            // then
            assertThat(assigned.getState()).isEqualTo(ASSIGNED);
        }

        @Test
        void returns_same_instance_when_already_assigned() {
            // given
            var equipment = equipment().type(MONITOR).state(ASSIGNED).build();

            // when
            var result = equipment.assign();

            // when
            assertThat(result).isSameAs(equipment);
        }

        @ParameterizedTest
        @EnumSource(value = EquipmentState.class, names = {"RESERVED", "ASSIGNED"}, mode = EXCLUDE)
        void throws_error_when_not_reserved(EquipmentState state) {
            // given
            var equipment = equipment().type(MONITOR).state(state).build();

            // when / then
            assertThatThrownBy(equipment::assign)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only reserved equipment can be assigned, but is: " + state);
        }
    }

    @Nested
    class MarkAvailableTests {

        @Test
        void marks_reserved_equipment_as_available() {
            // given
            var equipment = equipment().type(MONITOR).state(RESERVED).build();

            // when
            var available = equipment.markAvailable();

            // then
            assertThat(available.getState()).isEqualTo(AVAILABLE);
        }

        @Test
        void returns_same_instance_when_already_available() {
            // given
            var equipment = equipment().type(MONITOR).state(AVAILABLE).build();

            // when
            var result = equipment.markAvailable();

            // when
            assertThat(result).isSameAs(equipment);
        }

        @ParameterizedTest
        @EnumSource(value = EquipmentState.class, names = {"RESERVED", "ASSIGNED", "AVAILABLE"}, mode = EXCLUDE)
        void throws_error_when_equipment_is_in_invalid_state(EquipmentState state) {
            // given
            var equipment = equipment().type(MONITOR).state(state).build();

            // when / then
            assertThatThrownBy(equipment::markAvailable)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot mark available in state: %s", state);
        }
    }

    @Nested
    class RetireTests {

        String reason = "some reason";

        @Test
        void retires_available_equipment() {
            // given
            var equipment = equipment().type(MONITOR).state(AVAILABLE).build();

            // when
            var retired = equipment.retire(reason);

            // then
            assertThat(retired.getState()).isEqualTo(RETIRED);
            assertThat(retired.getRetireReason()).isEqualTo(reason);
        }

        @Test
        void returns_same_instance_when_already_retired() {
            // given
            var equipment = equipment().type(MONITOR).state(RETIRED).build();

            // when
            var result = equipment.retire(reason);

            // when
            assertThat(result).isSameAs(equipment);
        }

        @ParameterizedTest
        @EnumSource(value = EquipmentState.class, names = {"AVAILABLE", "RETIRED"}, mode = EXCLUDE)
        void throws_error_when_not_available(EquipmentState state) {
            // given
            var equipment = equipment().type(MONITOR).state(state).build();

            // when / then
            assertThatThrownBy(() -> equipment.retire(reason))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only available equipment can be retired, but is: " + state);
        }
    }
}
