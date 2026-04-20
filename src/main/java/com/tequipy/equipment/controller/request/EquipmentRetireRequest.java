package com.tequipy.equipment.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EquipmentRetireRequest(@NotBlank @Size(max = 300) String reason) {
}
