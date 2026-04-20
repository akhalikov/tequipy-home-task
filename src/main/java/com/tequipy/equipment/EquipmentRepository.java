package com.tequipy.equipment;

import com.tequipy.equipment.domain.Equipment;
import com.tequipy.equipment.domain.EquipmentState;
import com.tequipy.equipment.domain.EquipmentType;
import com.tequipy.equipment.error.EquipmentNotFoundException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;

public interface EquipmentRepository extends JpaRepository<Equipment, UUID> {

    default Equipment getBy(UUID id) {
        return findById(id).orElseThrow(() -> new EquipmentNotFoundException(id));
    }

    List<Equipment> findAllByState(EquipmentState state);

    @Lock(PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Equipment e WHERE e.state = 'AVAILABLE' AND e.type = :type ORDER BY e.conditionScore DESC")
    List<Equipment> findTopAvailableByTypeWithLock(@Param("type") EquipmentType type, Pageable pageable);
}
