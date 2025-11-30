package com.fipe.processor.presentation.dto;

public record VehicleResponse(
        Long id,
        String code,
        String brandCode,
        String model,
        String observations
) {}
