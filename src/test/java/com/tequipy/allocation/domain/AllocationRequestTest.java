package com.tequipy.allocation.domain;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.Set;

import static com.tequipy.TestData.equipment;
import static com.tequipy.TestData.randomEmployeeId;
import static com.tequipy.allocation.domain.AllocationState.ALLOCATED;
import static com.tequipy.allocation.domain.AllocationState.CANCELLED;
import static com.tequipy.allocation.domain.AllocationState.CONFIRMED;
import static com.tequipy.allocation.domain.AllocationState.FAILED;
import static com.tequipy.allocation.domain.AllocationState.PENDING;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;

class AllocationRequestTest {

    @Nested
    class AllocateTest {

        @Test
        void allocates_equipment() {
            // given
            var request = AllocationRequest.builder()
                .state(PENDING)
                .employeeId(randomEmployeeId())
                .build();

            var equipment = equipment().build();

            // when
            var allocated = request.allocate(List.of(equipment));

            // then
            assertThat(allocated).satisfies(actual -> {
                assertThat(actual.is(ALLOCATED)).isTrue();
                assertThat(actual.getAllocatedEquipments()).containsExactly(equipment);
            });
        }

        @Test
        void returns_same_instance_on_already_allocated_request_with_same_equipment() {
            // given
            var equipment1 = equipment().build();
            var equipment2 = equipment().build();
            var equipments = Set.of(equipment1, equipment2);
            var request = AllocationRequest.builder()
                .state(ALLOCATED)
                .employeeId(randomEmployeeId())
                .allocatedEquipments(equipments)
                .build();

            // when
            var result = request.allocate(equipments);

            // then
            assertThat(result).isSameAs(request);
        }

        @Test
        void throws_error_when_equipment_is_empty() {
            // given
            var request = AllocationRequest.builder()
                .state(PENDING)
                .employeeId(randomEmployeeId())
                .build();

           // when / then
           assertThatThrownBy(() -> request.allocate(emptyList()))
               .isInstanceOf(IllegalArgumentException.class)
               .hasMessage("Equipment can not be empty");
        }

        @ParameterizedTest
        @EnumSource(value = AllocationState.class, names = {"PENDING", "ALLOCATED"}, mode = EXCLUDE)
        void throws_error_when_state_is_invalid(AllocationState state) {
            // given
            var equipment = equipment().build();
            var request = AllocationRequest.builder()
                .state(state)
                .employeeId(randomEmployeeId())
                .build();

            // when / then
            assertThatThrownBy(() -> request.allocate(List.of(equipment)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Can't allocate in state: " + state);
        }
    }

    @Nested
    class ConfirmTests {

        @Test
        void confirms_allocated_request() {
            // given
            var request = AllocationRequest.builder()
                .state(ALLOCATED)
                .employeeId(randomEmployeeId())
                .build();

            // when
            var confirmed = request.confirm();

            // then
            assertThat(confirmed.is(CONFIRMED)).isTrue();
        }

        @Test
        void returns_same_instance_on_already_confirmed_request() {
            // given
            var request = AllocationRequest.builder()
                .state(CONFIRMED)
                .employeeId(randomEmployeeId())
                .build();

            // when
            var result = request.confirm();

            // then
            assertThat(result).isSameAs(request);
        }

        @ParameterizedTest
        @EnumSource(value = AllocationState.class, names = {"PENDING", "CANCELLED", "FAILED"})
        void throws_error_when_request_in_invalid_state(AllocationState state) {
            // given
            var request = AllocationRequest.builder()
                .state(state)
                .employeeId(randomEmployeeId())
                .build();

            // when
            assertThatThrownBy(request::confirm)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Can't confirm allocation in state: " + state);
        }
    }

    @Nested
    class CancelTests {

        @ParameterizedTest
        @EnumSource(value = AllocationState.class, names = {"PENDING", "ALLOCATED"})
        void cancels_allocation(AllocationState state) {
            // given
            var request = AllocationRequest.builder()
                .state(state)
                .employeeId(randomEmployeeId())
                .build();

            // when
            var cancelled = request.cancel();

            // then
            assertThat(cancelled.is(CANCELLED)).isTrue();
        }

        @Test
        void returns_same_instance_on_already_confirmed_request() {
            // given
            var request = AllocationRequest.builder()
                .state(CANCELLED)
                .employeeId(randomEmployeeId())
                .build();

            // when
            var result = request.cancel();

            // then
            assertThat(result).isSameAs(request);
        }

        @ParameterizedTest
        @EnumSource(value = AllocationState.class, names = {"PENDING", "ALLOCATED", "CANCELLED"}, mode = EXCLUDE)
        void throws_error_when_state_is_invalid(AllocationState state) {
            // given
            var request = AllocationRequest.builder()
                .state(state)
                .employeeId(randomEmployeeId())
                .build();

            // when / then
            assertThatThrownBy(request::cancel)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Can't cancel allocation in state: " + state);
        }
    }

    @Nested
    class FailTests {

        @Test
        void fails_pending_request_with_reason() {
            // given
            var reason = "Some reason";
            var request = AllocationRequest.builder()
                .state(PENDING)
                .employeeId(randomEmployeeId())
                .build();

            // when
            var cancelled = request.fail(reason);

            // then
            assertThat(cancelled).satisfies(actual -> {
                assertThat(actual.is(FAILED)).isTrue();
                assertThat(actual.getFailureReason()).isEqualTo(reason);
            });
        }

        @Test
        void returns_same_instance_on_already_failed_request_with_same_reason() {
            // given
            var reason = "some reason";
            var request = AllocationRequest.builder()
                .state(FAILED)
                .failureReason(reason)
                .employeeId(randomEmployeeId())
                .build();

            // when
            var result = request.fail(reason);

            // then
            assertThat(result).isSameAs(request);
        }

        @ParameterizedTest
        @EnumSource(value = AllocationState.class, names = {"PENDING", "FAILED"}, mode = EXCLUDE)
        void throws_error_when_state_is_invalid(AllocationState state) {
            // given
            var request = AllocationRequest.builder()
                .state(state)
                .employeeId(randomEmployeeId())
                .build();

            // when / then
            assertThatThrownBy(() -> request.fail("some reason"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Can't fail allocation in state: " + state);
        }
    }
}