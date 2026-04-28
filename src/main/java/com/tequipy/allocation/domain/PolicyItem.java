package com.tequipy.allocation.domain;

import com.tequipy.equipment.domain.Equipment;
import com.tequipy.equipment.domain.EquipmentType;
import lombok.Builder;

import java.math.BigDecimal;

import static java.util.Optional.ofNullable;

@Builder
public record PolicyItem(EquipmentType type, BigDecimal minConditionScore, String preferredBrand) {

    /**
     * Hard constraint
     */
    public boolean isSatisfiedBy(Equipment equipment) {
        return equipment.isSameType(type)
            && (ofNullable(minConditionScore).map(equipment::hasSufficientCondition).orElse(true));
    }

    /**
     * Soft preference
     */
    public boolean isOfPreferredBrand(Equipment equipment) {
        return ofNullable(preferredBrand).map(equipment::isSameBrand).orElse(false);
    }
}
