package com.tequipy.equipment.controller;

import com.tequipy.equipment.domain.EquipmentState;
import com.tequipy.equipment.service.EquipmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
        return EquipmentResponse.from(equipmentService.registerEquipment(request.toCommand()));
    }

    @GetMapping
    public List<EquipmentResponse> listEquipments(@RequestParam(required = false) EquipmentState state) {
        return ofNullable(state)
            .map(equipmentService::listEquipmentsByState)
            .orElseGet(equipmentService::listEquipments).stream()
            .map(EquipmentResponse::from)
            .toList();
    }
}
