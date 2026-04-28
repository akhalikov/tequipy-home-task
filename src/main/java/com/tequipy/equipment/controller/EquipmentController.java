package com.tequipy.equipment.controller;

import com.tequipy.equipment.EquipmentService;
import com.tequipy.equipment.controller.request.EquipmentRegisterRequest;
import com.tequipy.equipment.controller.request.EquipmentRetireRequest;
import com.tequipy.equipment.domain.Equipment;
import com.tequipy.equipment.domain.EquipmentState;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static java.util.Optional.ofNullable;

@RestController
@RequestMapping("/equipments")
public class EquipmentController {

    private final EquipmentService equipmentService;

    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EquipmentResponse registerEquipment(@Valid @RequestBody EquipmentRegisterRequest request) {
        final var equipment = Equipment.builder()
            .type(request.type())
            .brand(request.brand())
            .model(request.model())
            .conditionScore(request.conditionScore())
            .purchaseDate(request.purchaseDate())
            .build();

        return EquipmentResponse.from(equipmentService.registerEquipment(equipment));
    }

    @GetMapping
    public List<EquipmentResponse> listEquipments(@RequestParam(required = false) EquipmentState state) {
        return ofNullable(state)
            .map(equipmentService::listEquipmentsByState)
            .orElseGet(equipmentService::listEquipments).stream()
            .map(EquipmentResponse::from)
            .toList();
    }

    @PostMapping("/{id}/retire")
    public EquipmentResponse retireEquipment(@PathVariable UUID id, @Valid @RequestBody EquipmentRetireRequest request) {
        final var result = equipmentService.retireEquipment(id, request.reason());
        return EquipmentResponse.from(result);
    }
}
