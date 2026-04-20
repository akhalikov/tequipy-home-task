package com.tequipy;

import com.tequipy.allocation.domain.AllocationRequest;
import com.tequipy.allocation.domain.AllocationRequest.AllocationRequestBuilder;
import com.tequipy.allocation.domain.PolicyItem;
import com.tequipy.equipment.domain.Equipment;
import com.tequipy.equipment.domain.Equipment.EquipmentBuilder;

import java.util.List;

import static com.tequipy.equipment.domain.EquipmentType.MONITOR;
import static java.time.LocalDateTime.now;
import static java.util.UUID.randomUUID;

public class TestData {

    public static String randomEmployeeId() {
        return "employee-" + randomUUID();
    }

    public static AllocationRequestBuilder allocationRequest(String employeeId) {
        return AllocationRequest.builder()
            .id(randomUUID())
            .createdAt(now())
            .updatedAt(now())
            .employeeId(employeeId)
            .policy(List.of(PolicyItem.builder().type(MONITOR).build()));
    }

    public static EquipmentBuilder equipment() {
        return Equipment.builder()
            .id(randomUUID())
            .type(MONITOR);
    }
}
