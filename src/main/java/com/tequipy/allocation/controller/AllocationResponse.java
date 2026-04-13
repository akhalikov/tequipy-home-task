package com.tequipy.allocation.controller;

import com.tequipy.allocation.domain.AllocationRequest;
import com.tequipy.allocation.domain.AllocationState;
import com.tequipy.equipment.controller.EquipmentResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AllocationResponse(
    UUID id,
    String employeeId,
    AllocationState state,
    List<EquipmentResponse> allocatedEquipments,
    String failureReason,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static AllocationResponse from(AllocationRequest request) {
        return new AllocationResponse(
            request.getId(),
            request.getEmployeeId(),
            request.getState(),
            request.getAllocatedEquipments().stream()
                .map(EquipmentResponse::from)
                .toList(),
            request.getFailureReason(),
            request.getCreatedAt(),
            request.getUpdatedAt()
        );
    }
}
