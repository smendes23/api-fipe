package br.com.fipe.gateway.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Brand information")
public record BrandResponse(
        @Schema(description = "Brand code from FIPE", example = "1")
        String code,
        
        @Schema(description = "Brand name", example = "Acura")
        String name
) {}
