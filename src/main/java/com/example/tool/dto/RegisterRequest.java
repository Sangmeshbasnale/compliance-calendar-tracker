package com.example.tool.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Request body for registering a new user")
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
    @Schema(description = "Unique username (3–100 characters)", example = "alice",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    @Schema(description = "Password (minimum 6 characters)", example = "secret123",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Email(message = "Must be a valid email address")
    private String email;

    /**
     * Optional role assignment. Defaults to VIEWER if not provided.
     * Accepted values: ADMIN, MANAGER, VIEWER.
     */
    private String role;
}
