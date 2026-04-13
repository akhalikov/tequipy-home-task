package com.tequipy.allocation.domain;

import com.tequipy.equipment.domain.Equipment;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.tequipy.allocation.domain.AllocationState.ALLOCATED;
import static com.tequipy.allocation.domain.AllocationState.CANCELLED;
import static com.tequipy.allocation.domain.AllocationState.CONFIRMED;
import static com.tequipy.allocation.domain.AllocationState.PENDING;

@Entity
@Table(name = "allocation_request", indexes = {
    @Index(name = "idx_allocation_state", columnList = "state"),
    @Index(name = "idx_allocation_employee", columnList = "employee_id")
})
@Getter
@Setter
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

    @OneToMany(mappedBy = "allocationRequest", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PolicyItem> policy = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "allocation_equipment",
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

    public AllocationRequest allocate() {
        if (state != PENDING)
            throw new IllegalStateException("Cannot allocate in state: " + state);

        getAllocatedEquipments().forEach(Equipment::reserve);
        this.state = ALLOCATED;
        return this;
    }

    public AllocationRequest confirm() {
        if (state != ALLOCATED)
            throw new IllegalStateException("Cannot confirm allocation in state: " + state);

        getAllocatedEquipments().forEach(Equipment::assign);
        this.state = CONFIRMED;
        return this;
    }

    public AllocationRequest cancel() {
        if (state != ALLOCATED && state != CONFIRMED)
            throw new IllegalStateException("Cannot confirm allocation in state: " + state);

        getAllocatedEquipments().forEach(Equipment::markAvailable);
        this.state = CANCELLED;
        return this;
    }
}
