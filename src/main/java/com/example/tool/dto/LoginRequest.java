package com.example.tool.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Request body for user login")
public class LoginRequest {

    @NotBlank(message = "Username is required")
    @Schema(description = "Username of the registered user", example = "alice",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "Password is required")
    @Schema(description = "Password of the user", example = "secret123",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
}
