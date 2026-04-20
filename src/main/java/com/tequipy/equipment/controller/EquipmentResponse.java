package com.tequipy.equipment.controller;

import com.tequipy.equipment.domain.Equipment;
import com.tequipy.equipment.domain.EquipmentState;
import com.tequipy.equipment.domain.EquipmentType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static java.util.Optional.ofNullable;

public record EquipmentResponse(
    UUID id,
    EquipmentType type,
    String brand,
    String model,
    EquipmentState state,
    BigDecimal conditionScore,
    LocalDate purchaseDate,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    Optional<String> retireReason
) {
    public static EquipmentResponse from(Equipment equipment) {
        return new EquipmentResponse(
            equipment.getId(),
            equipment.getType(),
            equipment.getBrand(),
            equipment.getModel(),
            equipment.getState(),
            equipment.getConditionScore(),
            equipment.getPurchaseDate(),
            equipment.getCreatedAt(),
            equipment.getUpdatedAt(),
            ofNullable(equipment.getRetireReason())
        );
    }
}
