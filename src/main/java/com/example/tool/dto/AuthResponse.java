package com.example.tool.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Response body returned after successful login or registration")
public class AuthResponse {

    @Schema(description = "JWT Bearer token to use in Authorization header", example = "eyJhbGciOiJIUzI1NiJ9...")
    private final String token;

    @Schema(description = "Username of the authenticated user", example = "alice")
    private final String username;

    @Schema(description = "Role assigned to the user", example = "ROLE_VIEWER",
            allowableValues = {"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_VIEWER"})
    private final String role;
}
