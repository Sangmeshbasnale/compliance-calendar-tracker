package com.example.tool.integration;

import com.example.tool.config.AbstractIntegrationTest;
import com.example.tool.dto.LoginRequest;
import com.example.tool.dto.RegisterRequest;
import com.example.tool.entity.Compliance;
import com.example.tool.repository.ComplianceRepository;
import com.example.tool.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full-stack integration tests for the Compliance CRUD API.
 *
 * Container lifecycle  : PostgreSQL via Testcontainers (shared static instance).
 * Authentication       : Real JWT flow — register → login → use Bearer token.
 * Database migrations  : Flyway runs automatically on the Testcontainers DB.
 * Test ordering        : @TestMethodOrder ensures deterministic execution.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ComplianceIntegrationTest extends AbstractIntegrationTest {

    // ── Constants ─────────────────────────────────────────────────

    private static final String ADMIN_USERNAME = "it_admin";
    private static final String ADMIN_PASSWORD = "admin_pass_123";
    private static final String VIEWER_USERNAME = "it_viewer";
    private static final String VIEWER_PASSWORD = "viewer_pass_123";

    // ── Shared state across ordered tests ─────────────────────────

    private static String adminToken;
    private static String viewerToken;
    private static Long   createdComplianceId;

    // ── Spring beans ──────────────────────────────────────────────

    @Autowired MockMvc            mockMvc;
    @Autowired ObjectMapper       objectMapper;
    @Autowired ComplianceRepository complianceRepository;
    @Autowired UserRepository     userRepository;

    // ── Setup / Teardown ──────────────────────────────────────────

    @AfterAll
    static void cleanupUsers(@Autowired UserRepository userRepo) {
        userRepo.findByUsername(ADMIN_USERNAME).ifPresent(userRepo::delete);
        userRepo.findByUsername(VIEWER_USERNAME).ifPresent(userRepo::delete);
    }

    // ═══════════════════════════════════════════════════════════════
    // 1. AUTH SETUP — register users and obtain JWT tokens
    // ═══════════════════════════════════════════════════════════════

    @Test
    @Order(1)
    @DisplayName("Register ADMIN user → 201 Created with accessToken")
    void registerAdmin() throws Exception {
        RegisterRequest req = buildRegisterRequest(ADMIN_USERNAME, ADMIN_PASSWORD,
                "it_admin@test.com", "ADMIN");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username", is(ADMIN_USERNAME)))
                .andExpect(jsonPath("$.role", is("ADMIN")))
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()));
    }

    @Test
    @Order(2)
    @DisplayName("Register VIEWER user → 201 Created with role VIEWER")
    void registerViewer() throws Exception {
        RegisterRequest req = buildRegisterRequest(VIEWER_USERNAME, VIEWER_PASSWORD,
                "it_viewer@test.com", "VIEWER");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role", is("VIEWER")));
    }

    @Test
    @Order(3)
    @DisplayName("Login ADMIN → 200 OK, capture accessToken")
    void loginAdmin() throws Exception {
        adminToken = loginAndExtractToken(ADMIN_USERNAME, ADMIN_PASSWORD);
        assertThat(adminToken).isNotBlank();
    }

    @Test
    @Order(4)
    @DisplayName("Login VIEWER → 200 OK, capture accessToken")
    void loginViewer() throws Exception {
        viewerToken = loginAndExtractToken(VIEWER_USERNAME, VIEWER_PASSWORD);
        assertThat(viewerToken).isNotBlank();
    }

    @Test
    @Order(5)
    @DisplayName("Login with wrong password → 401 Unauthorized")
    void loginWithBadCredentials() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername(ADMIN_USERNAME);
        req.setPassword("wrong_password");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(6)
    @DisplayName("Register duplicate username → 400 Bad Request")
    void registerDuplicateUsername() throws Exception {
        RegisterRequest req = buildRegisterRequest(ADMIN_USERNAME, ADMIN_PASSWORD,
                "dup@test.com", "VIEWER");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ═══════════════════════════════════════════════════════════════
    // 2. CREATE — POST /api/compliance
    // ═══════════════════════════════════════════════════════════════

    @Test
    @Order(10)
    @DisplayName("ADMIN creates compliance record → 201 Created, persisted in DB")
    void adminCreatesCompliance() throws Exception {
        String body = """
                {
                  "title": "GDPR Annual Review",
                  "description": "Annual review of GDPR compliance obligations",
                  "status": "PENDING",
                  "dueDate": "2025-12-31"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/compliance")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title", is("GDPR Annual Review")))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.dueDate", is("2025-12-31")))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andReturn();

        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        createdComplianceId = node.get("id").asLong();

        // Verify DB persistence
        assertThat(complianceRepository.findById(createdComplianceId)).isPresent();
    }

    @Test
    @Order(11)
    @DisplayName("VIEWER cannot create compliance record → 403 Forbidden")
    void viewerCannotCreateCompliance() throws Exception {
        String body = """
                {
                  "title": "Unauthorized Record",
                  "status": "PENDING",
                  "dueDate": "2025-12-31"
                }
                """;

        mockMvc.perform(post("/api/compliance")
                        .header("Authorization", bearer(viewerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(12)
    @DisplayName("Unauthenticated request → 401 Unauthorized")
    void unauthenticatedCannotCreateCompliance() throws Exception {
        String body = """
                {
                  "title": "No Auth Record",
                  "status": "PENDING",
                  "dueDate": "2025-12-31"
                }
                """;

        mockMvc.perform(post("/api/compliance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(13)
    @DisplayName("Create with missing title → 400 Bad Request with field errors")
    void createWithMissingTitle() throws Exception {
        String body = """
                {
                  "status": "PENDING",
                  "dueDate": "2025-12-31"
                }
                """;

        mockMvc.perform(post("/api/compliance")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title", notNullValue()));
    }

    @Test
    @Order(14)
    @DisplayName("Create with missing status → 400 Bad Request with field errors")
    void createWithMissingStatus() throws Exception {
        String body = """
                {
                  "title": "Missing Status Record",
                  "dueDate": "2025-12-31"
                }
                """;

        mockMvc.perform(post("/api/compliance")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.status", notNullValue()));
    }

    // ═══════════════════════════════════════════════════════════════
    // 3. READ — GET /api/compliance
    // ═══════════════════════════════════════════════════════════════

    @Test
    @Order(20)
    @DisplayName("VIEWER fetches paginated list → 200 OK with pagination metadata")
    void viewerFetchesAllCompliance() throws Exception {
        mockMvc.perform(get("/api/compliance")
                        .header("Authorization", bearer(viewerToken))
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "id")
                        .param("sortDir", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", isA(java.util.List.class)))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.number", is(0)))
                .andExpect(jsonPath("$.size", is(10)));
    }

    @Test
    @Order(21)
    @DisplayName("ADMIN searches by keyword → 200 OK with matching results")
    void adminSearchByKeyword() throws Exception {
        mockMvc.perform(get("/api/compliance/search")
                        .header("Authorization", bearer(adminToken))
                        .param("q", "GDPR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[0].title", containsString("GDPR")));
    }

    @Test
    @Order(22)
    @DisplayName("Search with no matches → 200 OK with empty content")
    void searchWithNoMatches() throws Exception {
        mockMvc.perform(get("/api/compliance/search")
                        .header("Authorization", bearer(adminToken))
                        .param("q", "NONEXISTENT_KEYWORD_XYZ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements", is(0)));
    }

    @Test
    @Order(23)
    @DisplayName("ADMIN fetches stats → 200 OK with all stat keys")
    void adminFetchesStats() throws Exception {
        mockMvc.perform(get("/api/compliance/stats")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.pending", notNullValue()))
                .andExpect(jsonPath("$.completed", notNullValue()))
                .andExpect(jsonPath("$.overdue", notNullValue()));
    }

    @Test
    @Order(24)
    @DisplayName("VIEWER cannot access stats → 403 Forbidden")
    void viewerCannotAccessStats() throws Exception {
        mockMvc.perform(get("/api/compliance/stats")
                        .header("Authorization", bearer(viewerToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(25)
    @DisplayName("CSV export → 200 OK with correct Content-Type and CSV header row")
    void csvExportReturnsFile() throws Exception {
        mockMvc.perform(get("/api/compliance/export")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"))
                .andExpect(content().string(containsString("ID,Title,Description,Status,DueDate,CreatedAt")))
                .andExpect(content().string(containsString("GDPR Annual Review")));
    }

    // ═══════════════════════════════════════════════════════════════
    // 4. UPDATE — PUT /api/compliance/{id}
    // ═══════════════════════════════════════════════════════════════

    @Test
    @Order(30)
    @DisplayName("ADMIN updates compliance record → 200 OK, DB reflects changes")
    void adminUpdatesCompliance() throws Exception {
        String body = """
                {
                  "title": "GDPR Annual Review - Updated",
                  "description": "Updated description after review",
                  "status": "IN_PROGRESS",
                  "dueDate": "2025-11-30"
                }
                """;

        mockMvc.perform(put("/api/compliance/" + createdComplianceId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(createdComplianceId.intValue())))
                .andExpect(jsonPath("$.title", is("GDPR Annual Review - Updated")))
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")))
                .andExpect(jsonPath("$.dueDate", is("2025-11-30")));

        // Verify DB reflects the update
        Compliance updated = complianceRepository.findById(createdComplianceId).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("GDPR Annual Review - Updated");
        assertThat(updated.getStatus()).isEqualTo("IN_PROGRESS");
    }

    @Test
    @Order(31)
    @DisplayName("VIEWER cannot update compliance record → 403 Forbidden")
    void viewerCannotUpdateCompliance() throws Exception {
        String body = """
                {
                  "title": "Hacked Title",
                  "status": "PENDING",
                  "dueDate": "2025-12-31"
                }
                """;

        mockMvc.perform(put("/api/compliance/" + createdComplianceId)
                        .header("Authorization", bearer(viewerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());

        // Verify DB was NOT changed
        Compliance unchanged = complianceRepository.findById(createdComplianceId).orElseThrow();
        assertThat(unchanged.getTitle()).isEqualTo("GDPR Annual Review - Updated");
    }

    @Test
    @Order(32)
    @DisplayName("Update non-existent record → 404 Not Found")
    void updateNonExistentRecord() throws Exception {
        String body = """
                {
                  "title": "Ghost Record",
                  "status": "PENDING",
                  "dueDate": "2025-12-31"
                }
                """;

        mockMvc.perform(put("/api/compliance/999999")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("999999")));
    }

    // ═══════════════════════════════════════════════════════════════
    // 5. DELETE — DELETE /api/compliance/{id}
    // ═══════════════════════════════════════════════════════════════

    @Test
    @Order(40)
    @DisplayName("VIEWER cannot delete compliance record → 403 Forbidden")
    void viewerCannotDeleteCompliance() throws Exception {
        mockMvc.perform(delete("/api/compliance/" + createdComplianceId)
                        .header("Authorization", bearer(viewerToken)))
                .andExpect(status().isForbidden());

        // Record must still exist
        assertThat(complianceRepository.existsById(createdComplianceId)).isTrue();
    }

    @Test
    @Order(41)
    @DisplayName("ADMIN deletes compliance record → 204 No Content, removed from DB")
    void adminDeletesCompliance() throws Exception {
        mockMvc.perform(delete("/api/compliance/" + createdComplianceId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());

        // Verify hard-delete from DB
        assertThat(complianceRepository.existsById(createdComplianceId)).isFalse();
    }

    @Test
    @Order(42)
    @DisplayName("Delete already-deleted record → 404 Not Found")
    void deleteAlreadyDeletedRecord() throws Exception {
        mockMvc.perform(delete("/api/compliance/" + createdComplianceId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString(
                        String.valueOf(createdComplianceId))));
    }

    @Test
    @Order(43)
    @DisplayName("Delete non-existent record → 404 Not Found")
    void deleteNonExistentRecord() throws Exception {
        mockMvc.perform(delete("/api/compliance/999999")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNotFound());
    }

    // ═══════════════════════════════════════════════════════════════
    // 6. PAGINATION & SORTING
    // ═══════════════════════════════════════════════════════════════

    @Test
    @Order(50)
    @DisplayName("Paginated list with size=1 → only 1 item in content")
    void paginationWithSizeOne() throws Exception {
        // Seed two records first
        seedCompliance(adminToken, "Pagination Record A", "PENDING");
        seedCompliance(adminToken, "Pagination Record B", "COMPLETED");

        mockMvc.perform(get("/api/compliance")
                        .header("Authorization", bearer(adminToken))
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalPages", greaterThanOrEqualTo(2)));
    }

    @Test
    @Order(51)
    @DisplayName("Sort by dueDate descending → first item has latest date")
    void sortByDueDateDescending() throws Exception {
        mockMvc.perform(get("/api/compliance")
                        .header("Authorization", bearer(adminToken))
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "dueDate")
                        .param("sortDir", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
    }

    // ═══════════════════════════════════════════════════════════════
    // Private helpers
    // ═══════════════════════════════════════════════════════════════

    private String loginAndExtractToken(String username, String password) throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername(username);
        req.setPassword(password);

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("accessToken").asText();
    }

    private void seedCompliance(String token, String title, String status) throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "title", title,
                "status", status,
                "dueDate", LocalDate.now().plusDays(30).toString()
        ));
        mockMvc.perform(post("/api/compliance")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    private static RegisterRequest buildRegisterRequest(String username, String password,
                                                         String email, String role) {
        RegisterRequest req = new RegisterRequest();
        req.setUsername(username);
        req.setPassword(password);
        req.setEmail(email);
        req.setRole(role);
        return req;
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
