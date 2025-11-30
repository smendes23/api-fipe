package br.com.fipe.gateway.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Login credentials")
public record LoginRequest(
        @Schema(description = "Username", example = "admin")
        @NotBlank(message = "Username is required")
        String username,

        @Schema(description = "Password", example = "admin")
        @NotBlank(message = "Password is required")
        String password
) {}
