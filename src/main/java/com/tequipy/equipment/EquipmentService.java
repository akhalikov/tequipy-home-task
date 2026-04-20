package com.tequipy.equipment;

import com.tequipy.equipment.domain.Equipment;
import com.tequipy.equipment.domain.EquipmentState;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;

    public EquipmentService(EquipmentRepository equipmentRepository) {
        this.equipmentRepository = equipmentRepository;
    }

    @Transactional
    public Equipment registerEquipment(Equipment equipment) {
        return equipmentRepository.save(equipment);
    }

    @Transactional(readOnly = true)
    public List<Equipment> listEquipmentsByState(EquipmentState state) {
        return equipmentRepository.findAllByState(state);
    }

    @Transactional(readOnly = true)
    public List<Equipment> listEquipments() {
        return equipmentRepository.findAll();
    }

    @Transactional
    public Equipment retireEquipment(UUID equipmentId, String reason) {
        final var equipment = equipmentRepository.getBy(equipmentId);
        return equipmentRepository.save(equipment.retire(reason));
    }
}
