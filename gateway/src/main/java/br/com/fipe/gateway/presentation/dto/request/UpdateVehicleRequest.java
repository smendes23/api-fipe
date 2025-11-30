package br.com.fipe.gateway.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to update vehicle information")
public record UpdateVehicleRequest(
        @Schema(description = "New vehicle model name", example = "Integra GS 1.8 Special Edition")
        @Size(max = 200, message = "Model must not exceed 200 characters")
        String model,
        
        @Schema(description = "New observations", example = "Imported vehicle with custom modifications")
        @Size(max = 1000, message = "Observations must not exceed 1000 characters")
        String observations
) {}
