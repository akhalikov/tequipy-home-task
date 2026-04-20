package com.tequipy.allocation.service;

import com.tequipy.allocation.domain.AllocationCreatedEvent;
import com.tequipy.allocation.repository.AllocationRequestRepository;
import com.tequipy.allocation.service.algorithm.AllocationStrategy;
import com.tequipy.equipment.repository.EquipmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static com.tequipy.TestData.allocationRequest;
import static com.tequipy.TestData.equipment;
import static com.tequipy.allocation.domain.AllocationState.ALLOCATED;
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
    void creates_request_and_emits_event() {
        // given
        var employeeId = "employee-123";
        var request = allocationRequest(employeeId);
        var equipment = equipment();
        var allocated = request.allocate(List.of(equipment));
        given(allocationRequestRepository.save(request)).willReturn(allocated);

        // when
        var result = allocationService.createAllocationRequest(request);

        // then
        assertThat(result.getState()).isEqualTo(ALLOCATED);
        then(eventPublisher).should().publishEvent(new AllocationCreatedEvent(request.getId()));
    }
}