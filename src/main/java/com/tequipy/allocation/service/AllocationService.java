package com.tequipy.allocation.service;

import com.tequipy.allocation.controller.AllocationResponse;
import com.tequipy.allocation.domain.AllocationRequest;
import com.tequipy.allocation.repository.AllocationRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AllocationService {

    private final AllocationRequestRepository allocationRequestRepository;

    public AllocationService(AllocationRequestRepository allocationRequestRepository) {
        this.allocationRequestRepository = allocationRequestRepository;
    }

    @Transactional(readOnly = true)
    public AllocationRequest getAllocation(UUID id) {
        return allocationRequestRepository.getBy(id);
    }

    @Transactional
    public AllocationRequest allocate(AllocationRequest allocationRequest) {
        return allocationRequestRepository.save(allocationRequest);
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
}
