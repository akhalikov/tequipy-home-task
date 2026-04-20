package com.tequipy.allocation.controller;

import com.tequipy.allocation.AllocationService;
import com.tequipy.allocation.controller.request.AllocationRequest;
import com.tequipy.allocation.controller.request.PolicyItemRequest;
import com.tequipy.allocation.domain.PolicyItem;
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
    public AllocationResponse createAllocation(@Valid @RequestBody AllocationRequest request) {
        final var policyItems = request.policy().stream()
            .map(AllocationController::toEntityPolicyItem)
            .toList();

        final var allocationRequest = com.tequipy.allocation.domain.AllocationRequest.builder()
            .employeeId(request.employeeId())
            .policy(policyItems)
            .build();

        final var createdRequest = allocationService.createAllocationRequest(allocationRequest);
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

    private static PolicyItem toEntityPolicyItem(PolicyItemRequest request) {
        return PolicyItem.builder()
            .type(request.equipmentType())
            .minConditionScore(request.minConditionScore())
            .preferredBrand(request.preferredBrand())
            .build();
    }
}
