package com.fipe.processor.presentation.dto;

import jakarta.validation.constraints.Size;


public record UpdateVehicleRequest(
        @Size(max = 200, message = "Model must not exceed 200 characters")
        String model,
        @Size(max = 1000, message = "Observations must not exceed 1000 characters")
        String observations
) {}
