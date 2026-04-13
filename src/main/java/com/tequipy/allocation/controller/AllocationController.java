package com.tequipy.allocation.controller;

import com.tequipy.allocation.domain.AllocationRequest;
import com.tequipy.allocation.domain.PolicyItem;
import com.tequipy.allocation.service.AllocationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/allocations")
public class AllocationController {

    private final AllocationService allocationService;

    public AllocationController(AllocationService allocationService) {
        this.allocationService = allocationService;
    }

    @GetMapping("/{id}")
    public AllocationResponse getAllocation(@PathVariable UUID id) {
        final var request = allocationService.getAllocation(id);
        return AllocationResponse.from(request);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AllocationResponse createAllocation(@Valid @RequestBody AllocateEquipmentRequest request) {
        final var allocationRequest = AllocationRequest.builder()
            .employeeId(request.employeeId())
            .build();

        final var policyItems = request.policy().stream()
            .map(policyItem -> PolicyItem.builder()
                .allocationRequest(allocationRequest)
                .equipmentType(policyItem.equipmentType())
                .minConditionScore(policyItem.minConditionScore())
                .preferredBrand(policyItem.preferredBrand())
                .build())
            .toList();

        allocationRequest.getPolicy().addAll(policyItems);

        final var createdRequest = allocationService.allocate(allocationRequest);
        return AllocationResponse.from(createdRequest);
    }

    @PostMapping("/{id}/confirm")
    public AllocationResponse confirmAllocation(@PathVariable UUID id) {
        return AllocationResponse.from(allocationService.confirmAllocation(id));
    }

    @PostMapping("/{id}/cancel")
    public AllocationResponse cancelAllocation(@PathVariable UUID id) {
        return AllocationResponse.from(allocationService.cancelAllocation(id));
    }
}
