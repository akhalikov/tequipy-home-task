package com.tequipy.allocation.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import java.util.Collection;

@Builder(builderMethodName = "allocateEquipmentRequestBuilder")
public record AllocateEquipmentRequest(
    @NotBlank String employeeId,
    @NotEmpty @Valid Collection<PolicyItemRequest> policy
) {
}
