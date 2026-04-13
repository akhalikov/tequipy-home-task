package com.tequipy;

import com.tequipy.allocation.controller.PolicyItemRequest;
import com.tequipy.equipment.domain.EquipmentType;

import static com.tequipy.allocation.controller.PolicyItemRequest.policyItemRequestBuilder;
import static java.util.UUID.randomUUID;

public class TestData {

    public static String randomEmployeeId() {
        return "employee-" + randomUUID();
    }

    public static PolicyItemRequest policyItemRequest(EquipmentType type) {
        return policyItemRequestBuilder()
            .equipmentType(type)
            .build();
    }

    private TestData() {
    }
}
