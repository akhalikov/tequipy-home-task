package com.tequipy.allocation.error;

import java.util.UUID;

public class AllocationNotFoundException extends RuntimeException {

    public AllocationNotFoundException(UUID id) {
        super("Allocation request not found: " + id);
    }
}
