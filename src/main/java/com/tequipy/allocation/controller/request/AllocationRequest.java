package com.tequipy.allocation.controller.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import java.util.Collection;

@Builder
public record AllocationRequest(
    @NotBlank String employeeId,
    @NotEmpty @Valid Collection<PolicyItemRequest> policy
) {
}
