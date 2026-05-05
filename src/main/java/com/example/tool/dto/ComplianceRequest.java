package com.example.tool.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Schema(description = "Request body for creating or updating a compliance record")
public class ComplianceRequest {

    @NotBlank(message = "Title is required")
    @Schema(description = "Title of the compliance record", example = "GDPR Annual Review", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "Detailed description of the compliance requirement", example = "Annual review of GDPR data processing activities")
    private String description;

    @NotBlank(message = "Status is required")
    @Schema(description = "Current status of the record", example = "PENDING",
            allowableValues = {"PENDING", "COMPLETED", "OVERDUE", "OPEN", "CLOSED"},
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String status;

    @NotNull(message = "Due date is required")
    @Schema(description = "Due date for the compliance record (must not be in the past)", example = "2025-12-31",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate dueDate;
}
