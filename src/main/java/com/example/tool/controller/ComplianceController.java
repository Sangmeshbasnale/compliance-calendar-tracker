package com.example.tool.controller;

import com.example.tool.dto.ComplianceRequest;
import com.example.tool.dto.ComplianceResponse;
import com.example.tool.service.ComplianceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/compliance")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Compliance", description = "APIs for managing compliance records")
public class ComplianceController {

    private final ComplianceService complianceService;

    @Operation(
            summary = "Get all compliance records",
            description = "Returns a paginated list of all active (non-deleted) compliance records. Supports pagination and sorting.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Records retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<Page<ComplianceResponse>> getAll(
            @PageableDefault(size = 10, sort = "dueDate", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(complianceService.getAllRecords(pageable).map(ComplianceResponse::new));
    }

    @Operation(
            summary = "Get compliance record by ID",
            description = "Returns a single active compliance record by its ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Record found and returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @ApiResponse(responseCode = "404", description = "Record not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ComplianceResponse> getById(
            @Parameter(description = "ID of the compliance record", example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(new ComplianceResponse(complianceService.getRecordById(id)));
    }

    @Operation(
            summary = "Create a new compliance record",
            description = "Creates a new compliance record. Requires ROLE_ADMIN or ROLE_MANAGER.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Record created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<ComplianceResponse> create(@Valid @RequestBody ComplianceRequest request) {
        ComplianceResponse body = new ComplianceResponse(complianceService.createRecord(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(body.getId()).toUri();
        return ResponseEntity.created(location).body(body);
    }

    @Operation(
            summary = "Update a compliance record",
            description = "Updates an existing compliance record by ID. Requires ROLE_ADMIN or ROLE_MANAGER.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Record updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Record not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ComplianceResponse> update(
            @Parameter(description = "ID of the compliance record to update", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody ComplianceRequest request) {
        return ResponseEntity.ok(new ComplianceResponse(complianceService.updateRecord(id, request)));
    }

    @Operation(
            summary = "Soft delete a compliance record",
            description = "Marks a compliance record as deleted (soft delete). Requires ROLE_ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Record deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Record not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID of the compliance record to delete", example = "1")
            @PathVariable Long id) {
        complianceService.deleteRecord(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Search compliance records",
            description = "Case-insensitive search across title and description fields. Returns only active records.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search results returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/search")
    public ResponseEntity<List<ComplianceResponse>> search(
            @Parameter(description = "Search keyword for title or description", example = "GDPR")
            @RequestParam String q) {
        List<ComplianceResponse> results = complianceService.search(q)
                .stream().map(ComplianceResponse::new).toList();
        return ResponseEntity.ok(results);
    }

    @Operation(
            summary = "Get compliance statistics",
            description = "Returns counts of compliance records grouped by status (total, pending, completed, overdue, open, closed).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statistics returned successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> stats() {
        return ResponseEntity.ok(complianceService.getStats());
    }
}
