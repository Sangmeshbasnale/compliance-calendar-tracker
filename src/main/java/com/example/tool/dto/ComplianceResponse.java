package com.example.tool.dto;

import com.example.tool.entity.Compliance;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Schema(description = "Response body representing a compliance record")
public class ComplianceResponse {

    @Schema(description = "Unique identifier of the compliance record", example = "1")
    private final Long id;

    @Schema(description = "Title of the compliance record", example = "GDPR Annual Review")
    private final String title;

    @Schema(description = "Detailed description", example = "Annual review of GDPR data processing activities")
    private final String description;

    @Schema(description = "Current status", example = "PENDING",
            allowableValues = {"PENDING", "COMPLETED", "OVERDUE", "OPEN", "CLOSED"})
    private final String status;

    @Schema(description = "Due date for the compliance record", example = "2025-12-31")
    private final LocalDate dueDate;

    @Schema(description = "Whether the record has been soft-deleted", example = "false")
    private final boolean isDeleted;

    @Schema(description = "Timestamp when the record was created", example = "2025-01-15T10:30:00")
    private final LocalDateTime createdAt;

    @Schema(description = "Timestamp when the record was last updated", example = "2025-01-20T14:00:00")
    private final LocalDateTime updatedAt;

    public ComplianceResponse(Compliance c) {
        this.id          = c.getId();
        this.title       = c.getTitle();
        this.description = c.getDescription();
        this.status      = c.getStatus();
        this.dueDate     = c.getDueDate();
        this.isDeleted   = c.isDeleted();
        this.createdAt   = c.getCreatedAt();
        this.updatedAt   = c.getUpdatedAt();
    }
}
