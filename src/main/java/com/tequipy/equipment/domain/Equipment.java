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
import jakarta.persistence.Version;
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

@Entity
@Table(name = "equipment", indexes = {
    @Index(name = "idx_equipment_state", columnList = "state"),
    @Index(name = "idx_equipment_type", columnList = "type"),
    @Index(name = "idx_equipment_state_type", columnList = "state, type")
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
    private BigDecimal conditionScore;

    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;

    @Column(name = "retired_reason")
    private String retiredReason;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void markAvailable() {
        if (state != ASSIGNED && state != RESERVED)
            throw new IllegalStateException("Cannot mark available in state: " + state);

        state = AVAILABLE;
    }

    public void reserve() {
        if (state != AVAILABLE)
            throw new IllegalStateException("Only available equipment can be reserved");

        state = RESERVED;
    }

    public void assign() {
        if (state != RESERVED)
            throw new IllegalStateException("Only reserved equipment can be assigned");

        state = ASSIGNED;
    }
}
