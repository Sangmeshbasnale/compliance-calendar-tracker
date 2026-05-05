package com.example.tool.controller;

import com.example.tool.config.JwtAuthFilter;
import com.example.tool.config.JwtUtil;
import com.example.tool.dto.AuthResponse;
import com.example.tool.dto.LoginRequest;
import com.example.tool.dto.RegisterRequest;
import com.example.tool.exception.InvalidDataException;
import com.example.tool.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    private static final AuthResponse MOCK_RESPONSE =
            new AuthResponse("mock-jwt-token", "alice", "ROLE_VIEWER");

    @Nested
    @DisplayName("POST /auth/register")
    class Register {

        @Test
        @DisplayName("should return 200 with token on successful registration")
        void register_success_returns200() throws Exception {
            when(authService.register(any(RegisterRequest.class))).thenReturn(MOCK_RESPONSE);

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\": \"alice\", \"password\": \"secret123\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("mock-jwt-token"))
                    .andExpect(jsonPath("$.username").value("alice"))
                    .andExpect(jsonPath("$.role").value("ROLE_VIEWER"));
        }

        @Test
        @DisplayName("should return 400 when username already exists")
        void register_duplicateUsername_returns400() throws Exception {
            when(authService.register(any(RegisterRequest.class)))
                    .thenThrow(new InvalidDataException("Username already exists"));

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\": \"alice\", \"password\": \"secret123\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when username is blank")
        void register_blankUsername_returns400() throws Exception {
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\": \"\", \"password\": \"secret123\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when password is too short")
        void register_shortPassword_returns400() throws Exception {
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\": \"alice\", \"password\": \"abc\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /auth/login")
    class Login {

        @Test
        @DisplayName("should return 200 with token on successful login")
        void login_success_returns200() throws Exception {
            when(authService.login(any(LoginRequest.class))).thenReturn(MOCK_RESPONSE);

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\": \"alice\", \"password\": \"secret123\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("mock-jwt-token"))
                    .andExpect(jsonPath("$.username").value("alice"))
                    .andExpect(jsonPath("$.role").value("ROLE_VIEWER"));
        }

        @Test
        @DisplayName("should return 400 on bad credentials")
        void login_badCredentials_returns400() throws Exception {
            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new InvalidDataException("Invalid username or password"));

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\": \"alice\", \"password\": \"wrong\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when username is blank")
        void login_blankUsername_returns400() throws Exception {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\": \"\", \"password\": \"secret123\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when password is blank")
        void login_blankPassword_returns400() throws Exception {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\": \"alice\", \"password\": \"\"}"))
                    .andExpect(status().isBadRequest());
        }
    }
}
