package com.tequipy.allocation.strategy;

import com.tequipy.allocation.AllocationRequestRepository;
import com.tequipy.allocation.AllocationService;
import com.tequipy.allocation.domain.AllocationCreatedEvent;
import com.tequipy.equipment.EquipmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import static com.tequipy.TestData.allocationRequest;
import static com.tequipy.TestData.randomEmployeeId;
import static com.tequipy.allocation.domain.AllocationState.ALLOCATED;
import static com.tequipy.allocation.domain.AllocationState.PENDING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

class AllocationServiceTest {

    EquipmentRepository equipmentRepository = mock(EquipmentRepository.class);
    AllocationRequestRepository allocationRequestRepository = mock(AllocationRequestRepository.class);
    ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    AllocationStrategy strategy = mock(AllocationStrategy.class);
    AllocationService allocationService = new AllocationService(equipmentRepository, allocationRequestRepository, eventPublisher, strategy);

    @Test
    void creates_allocation_request_and_emits_event() {
        // given
        var employeeId = randomEmployeeId();
        var request = allocationRequest(employeeId).build();
        given(allocationRequestRepository.save(request)).willReturn(request);

        // when
        var result = allocationService.createAllocationRequest(request);

        // then
        assertThat(result.getState()).isEqualTo(PENDING);
        then(eventPublisher).should().publishEvent(new AllocationCreatedEvent(request.getId()));
    }

    @Test
    void does_not_allocate_and_skips_when_request_is_not_pending() {
        // given
        var employeeId = randomEmployeeId();
        var request = allocationRequest(employeeId).state(ALLOCATED).build();
        given(allocationRequestRepository.getBy(request.getId())).willReturn(request);

        // when
        allocationService.allocateEquipment(request.getId());

        // then
        then(strategy).shouldHaveNoInteractions();
    }
}