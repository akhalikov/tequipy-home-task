package com.tequipy.equipment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static com.tequipy.equipment.domain.EquipmentState.ASSIGNED;
import static com.tequipy.equipment.domain.EquipmentState.AVAILABLE;
import static com.tequipy.equipment.domain.EquipmentState.RESERVED;
import static com.tequipy.equipment.domain.EquipmentState.RETIRED;

@Entity
@Table(name = "equipment", indexes = {
    @Index(name = "idx_equipment_state", columnList = "state"),
    @Index(name = "idx_equipment_type", columnList = "type"),
    @Index(name = "idx_equipment_state_type_score", columnList = "state, type, condition_score DESC")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EquipmentType type;

    @Column(nullable = false, length = 100)
    private String brand;

    @Column(nullable = false, length = 100)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EquipmentState state = AVAILABLE;

    @Column(name = "condition_score", nullable = false, precision = 3, scale = 2)
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "1.0")
    private BigDecimal conditionScore;

    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;

    @Column(name = "retire_reason")
    private String retireReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public boolean isSameType(EquipmentType type) {
        return this.type == type;
    }

    public boolean isSameBrand(String brand) {
        return this.brand.equalsIgnoreCase(brand);
    }

    public boolean hasSufficientCondition(BigDecimal requiredScore) {
        return conditionScore.compareTo(requiredScore) >= 0;
    }

    private boolean is(EquipmentState state) {
        return this.state == state;
    }

    public Equipment markAvailable() {
        if (is(AVAILABLE))
            return this;

        if (!is(RESERVED) && !is(ASSIGNED))
            throw new IllegalStateException("Cannot mark available in state: " + state);

        state = AVAILABLE;
        return this;
    }

    public Equipment reserve() {
        if (!is(AVAILABLE))
            throw new IllegalStateException("Only available equipment can be reserved, but is: " + state);

        state = RESERVED;
        return this;
    }

    public Equipment assign() {
        if (is(ASSIGNED))
            return this;

        if (!is(RESERVED))
            throw new IllegalStateException("Only reserved equipment can be assigned, but is: " + state);

        state = ASSIGNED;
        return this;
    }

    public Equipment retire(String reason) {
        if (is(RETIRED))
            return this;

        if (!is(AVAILABLE))
            throw new IllegalStateException("Only available equipment can be retired, but is: " + state);

        this.state = RETIRED;
        this.retireReason = reason;
        return this;
    }
}
