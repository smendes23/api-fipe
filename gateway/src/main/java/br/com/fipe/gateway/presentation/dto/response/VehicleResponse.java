package br.com.fipe.gateway.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Vehicle information")
public record VehicleResponse(
        @Schema(description = "Vehicle unique identifier", example = "123e4567-e89b-12d3-a456-426614174000")
        Long id,
        
        @Schema(description = "Vehicle code from FIPE", example = "001004-1")
        String code,
        
        @Schema(description = "Brand code", example = "1")
        String brandCode,
        
        @Schema(description = "Vehicle model name", example = "Integra GS 1.8")
        String model,
        
        @Schema(description = "Additional observations", example = "Imported vehicle")
        String observations
) {}
