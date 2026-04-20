package com.tequipy.allocation.controller.request;

import com.tequipy.equipment.domain.EquipmentType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;

@Builder(builderMethodName = "policyItemRequestBuilder")
public record PolicyItemRequest(
    @NotNull EquipmentType equipmentType,
    @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal minConditionScore,
    String preferredBrand
) {
}
