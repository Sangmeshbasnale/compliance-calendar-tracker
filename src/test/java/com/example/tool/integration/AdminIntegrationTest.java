package com.example.tool.integration;

import com.example.tool.config.AbstractIntegrationTest;
import com.example.tool.dto.LoginRequest;
import com.example.tool.dto.RegisterRequest;
import com.example.tool.entity.Role;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for admin-only endpoints:
 * GET  /api/admin/users
 * PUT  /api/admin/users/{id}/role
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AdminIntegrationTest extends AbstractIntegrationTest {

    private static final String ADMIN_USERNAME  = "admin_ctrl_test";
    private static final String ADMIN_PASSWORD  = "admin_ctrl_pass";
    private static final String VIEWER_USERNAME = "viewer_ctrl_test";
    private static final String VIEWER_PASSWORD = "viewer_ctrl_pass";

    private static String adminToken;
    private static String viewerToken;
    private static Long   viewerUserId;

    @Autowired MockMvc       mockMvc;
    @Autowired ObjectMapper  objectMapper;
    @Autowired UserRepository userRepository;

    @AfterAll
    static void cleanup(@Autowired UserRepository userRepo) {
        userRepo.findByUsername(ADMIN_USERNAME).ifPresent(userRepo::delete);
        userRepo.findByUsername(VIEWER_USERNAME).ifPresent(userRepo::delete);
    }

    // ── Setup ─────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("Setup: register ADMIN and VIEWER, capture tokens and viewer ID")
    void setupUsersAndTokens() throws Exception {
        registerUser(ADMIN_USERNAME, ADMIN_PASSWORD, "ADMIN");
        registerUser(VIEWER_USERNAME, VIEWER_PASSWORD, "VIEWER");

        adminToken  = loginAndExtractToken(ADMIN_USERNAME, ADMIN_PASSWORD);
        viewerToken = loginAndExtractToken(VIEWER_USERNAME, VIEWER_PASSWORD);

        viewerUserId = userRepository.findByUsername(VIEWER_USERNAME)
                .orElseThrow().getId();

        assertThat(adminToken).isNotBlank();
        assertThat(viewerToken).isNotBlank();
        assertThat(viewerUserId).isPositive();
    }

    // ── GET /api/admin/users ──────────────────────────────────────

    @Test
    @Order(10)
    @DisplayName("ADMIN lists all users → 200 OK with id, username, role fields")
    void adminListsAllUsers() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)))
                .andExpect(jsonPath("$[*].id", everyItem(notNullValue())))
                .andExpect(jsonPath("$[*].username", everyItem(notNullValue())))
                .andExpect(jsonPath("$[*].role", everyItem(notNullValue())));
    }

    @Test
    @Order(11)
    @DisplayName("VIEWER cannot list users → 403 Forbidden")
    void viewerCannotListUsers() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", bearer(viewerToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(12)
    @DisplayName("Unauthenticated cannot list users → 401 Unauthorized")
    void unauthenticatedCannotListUsers() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    // ── PUT /api/admin/users/{id}/role ────────────────────────────

    @Test
    @Order(20)
    @DisplayName("ADMIN promotes VIEWER to MANAGER → 200 OK, DB role updated")
    void adminPromotesViewerToManager() throws Exception {
        String body = """
                { "role": "MANAGER" }
                """;

        mockMvc.perform(put("/api/admin/users/" + viewerUserId + "/role")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(viewerUserId.intValue())))
                .andExpect(jsonPath("$.username", is(VIEWER_USERNAME)))
                .andExpect(jsonPath("$.role", is("MANAGER")));

        // Verify DB reflects the role change
        Role updatedRole = userRepository.findByUsername(VIEWER_USERNAME)
                .orElseThrow().getRole();
        assertThat(updatedRole).isEqualTo(Role.MANAGER);
    }

    @Test
    @Order(21)
    @DisplayName("ADMIN demotes MANAGER back to VIEWER → 200 OK")
    void adminDemotesManagerToViewer() throws Exception {
        String body = """
                { "role": "VIEWER" }
                """;

        mockMvc.perform(put("/api/admin/users/" + viewerUserId + "/role")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role", is("VIEWER")));

        Role updatedRole = userRepository.findByUsername(VIEWER_USERNAME)
                .orElseThrow().getRole();
        assertThat(updatedRole).isEqualTo(Role.VIEWER);
    }

    @Test
    @Order(22)
    @DisplayName("VIEWER cannot change roles → 403 Forbidden")
    void viewerCannotChangeRole() throws Exception {
        String body = """
                { "role": "ADMIN" }
                """;

        mockMvc.perform(put("/api/admin/users/" + viewerUserId + "/role")
                        .header("Authorization", bearer(viewerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(23)
    @DisplayName("Change role with invalid value → 400 Bad Request")
    void changeRoleWithInvalidValue() throws Exception {
        String body = """
                { "role": "SUPERUSER" }
                """;

        mockMvc.perform(put("/api/admin/users/" + viewerUserId + "/role")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(24)
    @DisplayName("Change role for non-existent user → 400 Bad Request")
    void changeRoleForNonExistentUser() throws Exception {
        String body = """
                { "role": "VIEWER" }
                """;

        mockMvc.perform(put("/api/admin/users/999999/role")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ── Private helpers ───────────────────────────────────────────

    private void registerUser(String username, String password, String role) throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername(username);
        req.setPassword(password);
        req.setEmail(username + "@test.com");
        req.setRole(role);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

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

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
