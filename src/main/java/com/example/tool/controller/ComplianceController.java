package com.example.tool.controller;

import com.example.tool.dto.ComplianceRequest;
import com.example.tool.dto.ComplianceResponse;
import com.example.tool.entity.Compliance;
import com.example.tool.service.ComplianceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * REST controller for Compliance CRUD, search, stats, and CSV export.
 *
 * RBAC:
 *   VIEWER  → GET endpoints
 *   MANAGER → GET + POST + PUT
 *   ADMIN   → all including DELETE
 */
@RestController
@RequestMapping("/api/compliance")
@RequiredArgsConstructor
public class ComplianceController {

    private final ComplianceService complianceService;

    // ── GET all (paginated) ───────────────────────────────────────

    /**
     * GET /api/compliance?page=0&size=10&sortBy=id&sortDir=asc
     *
     * Sample response:
     * {
     *   "content": [{ "id":1, "title":"GDPR Review", "status":"PENDING", "dueDate":"2025-12-31" }],
     *   "totalElements": 1, "totalPages": 1, "number": 0, "size": 10
     * }
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER','MANAGER','ADMIN')")
    public Page<ComplianceResponse> getAll(
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "10")  int size,
            @RequestParam(defaultValue = "id")  String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return complianceService.getAll(page, size, sortBy, sortDir)
                .map(ComplianceResponse::new);
    }

    // ── POST create ───────────────────────────────────────────────

    /**
     * POST /api/compliance
     * Body: { "title":"GDPR Review", "status":"PENDING", "dueDate":"2025-12-31" }
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ComplianceResponse create(@Valid @RequestBody ComplianceRequest request) {
        return new ComplianceResponse(complianceService.create(request));
    }

    // ── PUT update ────────────────────────────────────────────────

    /**
     * PUT /api/compliance/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ComplianceResponse update(@PathVariable Long id,
                                     @Valid @RequestBody ComplianceRequest request) {
        return new ComplianceResponse(complianceService.update(id, request));
    }

    // ── DELETE ────────────────────────────────────────────────────

    /**
     * DELETE /api/compliance/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        complianceService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── GET search ────────────────────────────────────────────────

    /**
     * GET /api/compliance/search?q=gdpr&page=0&size=10
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('VIEWER','MANAGER','ADMIN')")
    public Page<ComplianceResponse> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "10")  int size,
            @RequestParam(defaultValue = "id")  String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return complianceService.search(q, page, size, sortBy, sortDir)
                .map(ComplianceResponse::new);
    }

    // ── GET stats ─────────────────────────────────────────────────

    /**
     * GET /api/compliance/stats
     * Response: { "total":10, "pending":4, "completed":5, "overdue":1 }
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public Map<String, Long> stats() {
        return complianceService.getStats();
    }

    // ── GET export CSV ────────────────────────────────────────────

    /**
     * GET /api/compliance/export
     * Returns a downloadable CSV file with all compliance records.
     */
    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('VIEWER','MANAGER','ADMIN')")
    public ResponseEntity<byte[]> exportToCsv() {
        List<Compliance> items = complianceService.exportAll();

        StringBuilder csv = new StringBuilder("ID,Title,Description,Status,DueDate,CreatedAt\n");
        for (Compliance c : items) {
            csv.append(c.getId()).append(',')
               .append(escape(c.getTitle())).append(',')
               .append(escape(c.getDescription())).append(',')
               .append(c.getStatus()).append(',')
               .append(c.getDueDate() != null ? c.getDueDate() : "").append(',')
               .append(c.getCreatedAt() != null ? c.getCreatedAt() : "").append('\n');
        }

        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDispositionFormData("attachment", "compliances.csv");
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    // ── Helper ────────────────────────────────────────────────────

    private String escape(String value) {
        if (value == null) return "";
        String cleaned = value.replaceAll("\\R", " ");
        if (cleaned.contains(",") || cleaned.contains("\"")) {
            cleaned = "\"" + cleaned.replace("\"", "\"\"") + "\"";
        }
        return cleaned;
    }
}
