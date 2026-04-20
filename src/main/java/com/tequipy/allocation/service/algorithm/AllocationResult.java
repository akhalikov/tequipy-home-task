package com.tequipy.allocation.service.algorithm;

import com.tequipy.allocation.domain.PolicyItem;
import com.tequipy.equipment.domain.Equipment;

import java.util.Map;

public sealed interface AllocationResult permits AllocationResult.Success, AllocationResult.Failure {

    record Success(Map<PolicyItem, Equipment> assignment, double totalScore) implements AllocationResult {}

    record Failure(String reason) implements AllocationResult {}

    static Success success(Map<PolicyItem, Equipment> assignment, double totalScore) {
        return new Success(assignment, totalScore);
    }

    static Failure failure(String reason) {
        return new Failure(reason);
    }
}
