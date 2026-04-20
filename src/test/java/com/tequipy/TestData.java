package com.tequipy;

import com.tequipy.allocation.domain.AllocationRequest;
import com.tequipy.allocation.domain.PolicyItem;
import com.tequipy.equipment.domain.Equipment;

import java.util.List;

import static com.tequipy.equipment.domain.EquipmentType.MONITOR;
import static java.time.LocalDateTime.now;
import static java.util.UUID.randomUUID;

public class TestData {

    public static String randomEmployeeId() {
        return "employee-" + randomUUID();
    }

    public static AllocationRequest allocationRequest(String employeeId) {
        return AllocationRequest.builder()
            .id(randomUUID())
            .createdAt(now())
            .updatedAt(now())
            .employeeId(employeeId)
            .policy(List.of(PolicyItem.builder().type(MONITOR).build()))
            .build();
    }

    public static Equipment.EquipmentBuilder equipment() {
        return Equipment.builder()
            .id(randomUUID())
            .type(MONITOR);
    }
}
