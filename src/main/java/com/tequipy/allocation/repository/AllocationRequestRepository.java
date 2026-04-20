package com.tequipy.allocation.repository;

import com.tequipy.allocation.domain.AllocationRequest;
import com.tequipy.allocation.error.AllocationNotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AllocationRequestRepository extends JpaRepository<AllocationRequest, UUID> {

    default AllocationRequest getBy(UUID id) {
        return findByIdWithDetails(id).orElseThrow(() -> new AllocationNotFoundException(id));
    }

    @Query("SELECT ar FROM AllocationRequest ar " +
        "LEFT JOIN FETCH ar.allocatedEquipments " +
        "WHERE ar.id = :id")
    Optional<AllocationRequest> findByIdWithDetails(@Param("id") UUID id);
}
