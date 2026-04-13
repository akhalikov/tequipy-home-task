package com.tequipy.equipment.service;

import com.tequipy.equipment.domain.Equipment;
import com.tequipy.equipment.domain.EquipmentState;
import com.tequipy.equipment.repository.EquipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;

    public EquipmentService(EquipmentRepository equipmentRepository) {
        this.equipmentRepository = equipmentRepository;
    }

    @Transactional
    public Equipment registerEquipment(EquipmentRegisterCommand command) {
        return equipmentRepository.save(command.toEquipment());
    }

    @Transactional(readOnly = true)
    public List<Equipment> listEquipmentsByState(EquipmentState state) {
        return equipmentRepository.findAllByState(state);
    }

    @Transactional(readOnly = true)
    public List<Equipment> listEquipments() {
        return equipmentRepository.findAll();
    }
}
