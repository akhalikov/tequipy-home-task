package com.tequipy.allocation.service;

import com.tequipy.allocation.domain.AllocationCreatedEvent;
import com.tequipy.allocation.domain.AllocationRequest;
import com.tequipy.allocation.repository.AllocationRequestRepository;
import com.tequipy.allocation.service.algorithm.AllocationResult.Failure;
import com.tequipy.allocation.service.algorithm.AllocationResult.Success;
import com.tequipy.allocation.service.algorithm.AllocationStrategy;
import com.tequipy.equipment.domain.Equipment;
import com.tequipy.equipment.repository.EquipmentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.UUID;

import static com.tequipy.allocation.domain.AllocationState.PENDING;

@Service
@Slf4j
public class AllocationService {

    private final EquipmentRepository equipmentRepository;
    private final AllocationRequestRepository allocationRequestRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AllocationStrategy allocationStrategy;

    public AllocationService(EquipmentRepository equipmentRepository,
                             AllocationRequestRepository allocationRequestRepository,
                             ApplicationEventPublisher eventPublisher,
                             AllocationStrategy allocationStrategy) {
        this.equipmentRepository = equipmentRepository;
        this.allocationRequestRepository = allocationRequestRepository;
        this.eventPublisher = eventPublisher;
        this.allocationStrategy = allocationStrategy;
    }

    @Transactional(readOnly = true)
    public AllocationRequest getAllocation(UUID id) {
        return allocationRequestRepository.getBy(id);
    }

    @Transactional
    public AllocationRequest createAllocationRequest(AllocationRequest allocationRequest) {
        final var created = allocationRequestRepository.save(allocationRequest);
        eventPublisher.publishEvent(new AllocationCreatedEvent(created.getId()));
        return created;
    }

    @Transactional
    public void allocateEquipment(UUID requestId) {
        final var allocationRequest = allocationRequestRepository.getBy(requestId);
        if (allocationRequest.getState() != PENDING) {
            log.warn("Allocation {} is not PENDING, skipping", requestId);
            return;
        }
        final var availableEquipment = equipmentRepository.findAllAvailableWithLock();
        final var policy = allocationRequest.getPolicy();
        final var result = allocationStrategy.allocate(policy, availableEquipment);
        switch (result) {
            case Failure failure -> failAllocation(allocationRequest, failure.reason());
            case Success success -> markAllocated(allocationRequest, success);
        }
    }

    @Transactional
    public AllocationRequest confirmAllocation(UUID id) {
        final var request = allocationRequestRepository.getBy(id);
        return allocationRequestRepository.save(request.confirm());
    }

    @Transactional
    public AllocationRequest cancelAllocation(UUID id) {
        final var request = allocationRequestRepository.getBy(id);
        return allocationRequestRepository.save(request.cancel());
    }

    private void failAllocation(AllocationRequest request, String reason) {
        log.warn("Failing allocation request id={} due to: {}", request.getId(), reason);
        allocationRequestRepository.save(request.fail(reason));
    }

    private void markAllocated(AllocationRequest request, Success allocationResult) {
        log.info("Allocation request id={} succeeded with score: {}", request.getId(), allocationResult.totalScore());
        final var reservedEquipment = reserveEquipment(allocationResult);
        final var allocated = request.allocate(reservedEquipment);
        allocationRequestRepository.save(allocated);
    }

    private Collection<Equipment> reserveEquipment(Success allocationResult) {
        final var reservedEquipment = allocationResult.assignment().values().stream()
            .map(Equipment::reserve)
            .toList();

        return equipmentRepository.saveAll(reservedEquipment);
    }
}
