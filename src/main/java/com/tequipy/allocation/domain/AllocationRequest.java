package com.tequipy.allocation.domain;

import com.tequipy.equipment.domain.Equipment;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.tequipy.allocation.domain.AllocationState.ALLOCATED;
import static com.tequipy.allocation.domain.AllocationState.CANCELLED;
import static com.tequipy.allocation.domain.AllocationState.CONFIRMED;
import static com.tequipy.allocation.domain.AllocationState.FAILED;
import static com.tequipy.allocation.domain.AllocationState.PENDING;
import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "allocation_requests", indexes = {
    @Index(name = "idx_allocation_state", columnList = "state"),
    @Index(name = "idx_allocation_employee", columnList = "employee_id")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllocationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "employee_id", nullable = false)
    private String employeeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AllocationState state = PENDING;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private List<PolicyItem> policy = new ArrayList<>();

    @ManyToMany(fetch = LAZY)
    @JoinTable(
        name = "allocated_equipment",
        joinColumns = @JoinColumn(name = "allocation_request_id"),
        inverseJoinColumns = @JoinColumn(name = "equipment_id")
    )
    @Builder.Default
    private Set<Equipment> allocatedEquipments = new LinkedHashSet<>();

    @Column(name = "failure_reason")
    private String failureReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public boolean is(AllocationState state) {
        return this.state == state;
    }

    public boolean isOneOf(AllocationState... state) {
        return Set.of(state).contains(this.state);
    }

    public AllocationRequest allocate(Collection<Equipment> equipment) {
        if (is(ALLOCATED) && this.allocatedEquipments.containsAll(equipment) && equipment.containsAll(this.allocatedEquipments))
            return this;

        if (equipment.isEmpty())
            throw new IllegalArgumentException("Equipment can not be empty");

        if (!is(PENDING))
            throw new IllegalStateException("Can't allocate in state: " + state);

        this.allocatedEquipments.addAll(equipment);
        this.state = ALLOCATED;
        return this;
    }

    public AllocationRequest confirm() {
        if (is(CONFIRMED))
            return this;

        if (!is(ALLOCATED))
            throw new IllegalStateException("Can't confirm allocation in state: " + state);

        getAllocatedEquipments().forEach(Equipment::assign);
        this.state = CONFIRMED;
        return this;
    }

    public AllocationRequest cancel() {
        if (is(CANCELLED))
            return this;

        if (!isOneOf(PENDING, ALLOCATED))
            throw new IllegalStateException("Can't cancel allocation in state: " + state);

        getAllocatedEquipments().forEach(Equipment::markAvailable);
        this.state = CANCELLED;
        return this;
    }

    public AllocationRequest fail(String reason) {
        if (is(FAILED) && this.failureReason.equals(reason))
            return this;

        if (!is(PENDING))
            throw new IllegalStateException("Can't fail allocation in state: " + state);

        this.failureReason = reason;
        this.state = FAILED;
        return this;
    }
}
