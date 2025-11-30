package br.com.fipe.gateway.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Authentication response with JWT token")
public record TokenResponse (
        @Schema(description = "JWT access token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String accessToken,

        @Schema(description = "Token type", example = "Bearer")
        String tokenType,

        @Schema(description = "Token expiration time in milliseconds", example = "86400000")
        Long expiresIn
){}
