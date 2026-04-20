package com.tequipy.allocation;

import com.tequipy.allocation.domain.AllocationCreatedEvent;
import com.tequipy.allocation.domain.AllocationRequest;
import com.tequipy.allocation.domain.PolicyItem;
import com.tequipy.allocation.strategy.AllocationResult.Failure;
import com.tequipy.allocation.strategy.AllocationResult.Success;
import com.tequipy.allocation.strategy.AllocationStrategy;
import com.tequipy.equipment.domain.Equipment;
import com.tequipy.equipment.EquipmentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.tequipy.allocation.domain.AllocationState.PENDING;
import static com.tequipy.allocation.strategy.Constants.CANDIDATES_PER_SLOT;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.toSet;

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
        final var policy = allocationRequest.getPolicy();
        final var availableEquipment = findCandidatesForPolicy(policy);
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

    private Collection<Equipment> findCandidatesForPolicy(List<PolicyItem> policy) {
        final var slotsPerType = policy.stream().collect(Collectors.groupingBy(PolicyItem::type, counting()));
        return slotsPerType.entrySet().stream()
            .flatMap(entry -> equipmentRepository.findTopAvailableByTypeWithLock(entry.getKey(), PageRequest.of(0, limitFor(entry.getValue()))).stream())
            .collect(toSet());
    }

    private static int limitFor(long count) {
        return (int) (count * CANDIDATES_PER_SLOT);
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
