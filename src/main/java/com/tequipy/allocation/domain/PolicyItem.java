package com.tequipy.allocation.domain;

import com.tequipy.equipment.domain.Equipment;
import com.tequipy.equipment.domain.EquipmentType;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.Optional;

@Builder
public record PolicyItem(EquipmentType type,
                         Optional<BigDecimal> minConditionScore,
                         Optional<String> preferredBrand) {

    public boolean isSatisfiedBy(Equipment equipment) {
        return equipment.isSameType(type)
            && minConditionScore.map(equipment::hasSufficientCondition).orElse(true);
    }

    public boolean isOfPreferredBrand(Equipment equipment) {
        return preferredBrand.map(equipment::isSameBrand).orElse(false);
    }
}
