package com.tequipy.equipment.repository;

import com.tequipy.equipment.domain.Equipment;
import com.tequipy.equipment.domain.EquipmentState;
import com.tequipy.equipment.domain.EquipmentType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface EquipmentRepository extends JpaRepository<Equipment, UUID> {

    List<Equipment> findAllByState(EquipmentState state);

    List<Equipment> findAllByStateAndType(EquipmentState state, EquipmentType type);

    /**
     * Fetches all AVAILABLE equipment with a pessimistic write lock to prevent
     * concurrent allocation from selecting the same equipment.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Equipment e WHERE e.state = 'AVAILABLE' ORDER BY e.conditionScore DESC")
    List<Equipment> findAllAvailableWithLock();
}
