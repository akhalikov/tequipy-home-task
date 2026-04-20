package com.tequipy.equipment.error;

import java.util.UUID;

public class EquipmentNotFoundException extends RuntimeException {

    public EquipmentNotFoundException(UUID id) {
        super("Equipment not found: " + id);
    }
}
