package com.tequipy.allocation.event;

import com.tequipy.allocation.domain.AllocationCreatedEvent;
import com.tequipy.allocation.service.AllocationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

@Component
@Slf4j
public class AllocationCreatedEventListener {

    private final AllocationService allocationService;

    public AllocationCreatedEventListener(AllocationService allocationService) {
        this.allocationService = allocationService;
    }

    @Async
    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void handleAllocationCreated(AllocationCreatedEvent event) {
        log.info("Processing allocation request: {}", event.id());
        allocationService.allocateEquipment(event.id());
    }
}
