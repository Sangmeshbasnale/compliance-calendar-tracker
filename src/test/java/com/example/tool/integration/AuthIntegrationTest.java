package com.example.tool.integration;

import com.example.tool.config.AbstractIntegrationTest;
import com.example.tool.dto.LoginRequest;
import com.example.tool.dto.RefreshTokenRequest;
import com.example.tool.dto.RegisterRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for authentication endpoints:
 * POST /auth/register, /auth/login, /auth/refresh
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthIntegrationTest extends AbstractIntegrationTest {

    private static final String USERNAME = "auth_test_user";
    private static final String PASSWORD = "secure_pass_456";
    private static final String EMAIL    = "auth_test@test.com";

    private static String capturedRefreshToken;

    @Autowired MockMvc       mockMvc;
    @Autowired ObjectMapper  objectMapper;
    @Autowired UserRepository userRepository;

    @AfterAll
    static void cleanup(@Autowired UserRepository userRepo) {
        userRepo.findByUsername(USERNAME).ifPresent(userRepo::delete);
    }

    // ── Register ──────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("Register new user → 201 Created with tokens and role")
    void registerNewUser() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername(USERNAME);
        req.setPassword(PASSWORD);
        req.setEmail(EMAIL);
        req.setRole("MANAGER");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username", is(USERNAME)))
                .andExpect(jsonPath("$.role", is("MANAGER")))
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()));

        // Verify user persisted in DB
        assertThat(userRepository.findByUsername(USERNAME)).isPresent();
    }

    @Test
    @Order(2)
    @DisplayName("Register without role → defaults to VIEWER")
    void registerDefaultsToViewer() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername(USERNAME + "_viewer");
        req.setPassword(PASSWORD);
        req.setEmail("viewer2@test.com");
        // role intentionally omitted

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role", is("VIEWER")));

        userRepository.findByUsername(USERNAME + "_viewer").ifPresent(userRepository::delete);
    }

    @Test
    @Order(3)
    @DisplayName("Register with blank username → 400 Bad Request")
    void registerBlankUsername() throws Exception {
        String body = """
                { "username": "", "password": "pass123", "email": "x@x.com" }
                """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username", notNullValue()));
    }

    @Test
    @Order(4)
    @DisplayName("Register with short password → 400 Bad Request")
    void registerShortPassword() throws Exception {
        String body = """
                { "username": "newuser", "password": "abc", "email": "x@x.com" }
                """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password", notNullValue()));
    }

    @Test
    @Order(5)
    @DisplayName("Register with invalid email format → 400 Bad Request")
    void registerInvalidEmail() throws Exception {
        String body = """
                { "username": "emailtest", "password": "pass123", "email": "not-an-email" }
                """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email", notNullValue()));
    }

    @Test
    @Order(6)
    @DisplayName("Register duplicate username → 400 Bad Request")
    void registerDuplicateUsername() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername(USERNAME);
        req.setPassword(PASSWORD);
        req.setEmail("dup@test.com");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ── Login ─────────────────────────────────────────────────────

    @Test
    @Order(10)
    @DisplayName("Login with valid credentials → 200 OK with accessToken and refreshToken")
    void loginSuccess() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername(USERNAME);
        req.setPassword(PASSWORD);

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.username", is(USERNAME)))
                .andExpect(jsonPath("$.role", is("MANAGER")))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        capturedRefreshToken = body.get("refreshToken").asText();
        assertThat(capturedRefreshToken).isNotBlank();
    }

    @Test
    @Order(11)
    @DisplayName("Login with wrong password → 401 Unauthorized")
    void loginWrongPassword() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername(USERNAME);
        req.setPassword("wrong_password");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(12)
    @DisplayName("Login with non-existent user → 401 Unauthorized")
    void loginNonExistentUser() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername("ghost_user_xyz");
        req.setPassword("any_password");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(13)
    @DisplayName("Login with blank password → 400 Bad Request")
    void loginBlankPassword() throws Exception {
        String body = """
                { "username": "someuser", "password": "" }
                """;

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password", notNullValue()));
    }

    // ── Refresh Token ─────────────────────────────────────────────

    @Test
    @Order(20)
    @DisplayName("Refresh with valid refresh token → 200 OK with new accessToken")
    void refreshTokenSuccess() throws Exception {
        assertThat(capturedRefreshToken)
                .as("refreshToken must be captured in loginSuccess first")
                .isNotBlank();

        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken(capturedRefreshToken);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", is(capturedRefreshToken)));
    }

    @Test
    @Order(21)
    @DisplayName("Refresh with invalid token → 401 Unauthorized")
    void refreshWithInvalidToken() throws Exception {
        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("this.is.not.a.valid.jwt");

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }
}
