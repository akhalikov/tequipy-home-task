package com.tequipy.allocation.service.algorithm;

import com.tequipy.allocation.domain.PolicyItem;
import com.tequipy.equipment.domain.Equipment;

import java.util.Collection;
import java.util.List;

public interface AllocationStrategy {

    AllocationResult allocate(List<PolicyItem> policy, Collection<Equipment> equipment);
}
