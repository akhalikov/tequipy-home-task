package com.tequipy.equipment.controller;

import com.tequipy.equipment.domain.EquipmentType;
import com.tequipy.equipment.service.EquipmentRegisterCommand;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record EquipmentRegisterRequest(
    @NotNull EquipmentType type,
    @NotBlank @Size(max = 100) String brand,
    @NotBlank @Size(max = 100) String model,
    @NotNull @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal conditionScore,
    @NotNull @PastOrPresent LocalDate purchaseDate
) {

    public EquipmentRegisterCommand toCommand() {
        return EquipmentRegisterCommand.builder()
            .type(this.type)
            .brand(this.brand)
            .model(this.model)
            .conditionScore(this.conditionScore)
            .purchaseDate(this.purchaseDate)
            .build();
    }
}
