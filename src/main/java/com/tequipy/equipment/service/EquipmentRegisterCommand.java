package com.tequipy.equipment.service;

import com.tequipy.equipment.domain.Equipment;
import com.tequipy.equipment.domain.EquipmentType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record EquipmentRegisterCommand(EquipmentType type,
                                       String brand,
                                       String model,
                                       BigDecimal conditionScore,
                                       LocalDate purchaseDate) {

    public Equipment toEquipment() {
        return Equipment.builder()
            .type(type)
            .brand(brand)
            .model(model)
            .conditionScore(conditionScore)
            .purchaseDate(purchaseDate)
            .build();
    }
}
