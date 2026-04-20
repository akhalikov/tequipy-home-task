package com.tequipy.equipment.controller;

import com.tequipy.equipment.domain.Equipment;
import com.tequipy.equipment.domain.EquipmentState;
import com.tequipy.equipment.domain.EquipmentType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record EquipmentResponse(
    UUID id,
    EquipmentType type,
    String brand,
    String model,
    EquipmentState state,
    BigDecimal conditionScore,
    LocalDate purchaseDate,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
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
            equipment.getUpdatedAt()
        );
    }
}
