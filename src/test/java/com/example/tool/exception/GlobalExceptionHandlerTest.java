package com.example.tool.exception;

import com.example.tool.config.JwtAuthFilter;
import com.example.tool.config.JwtUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GlobalExceptionHandlerTest.TestController.class)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @RestController
    @RequestMapping("/test-ex")
    static class TestController {

        @GetMapping("/not-found")
        public void notFound() {
            throw new ResourceNotFoundException("Resource not found");
        }

        @GetMapping("/compliance-not-found")
        public void complianceNotFound() {
            throw new ComplianceNotFoundException(42L);
        }

        @GetMapping("/invalid-data")
        public void invalidData() {
            throw new InvalidDataException("Invalid input provided");
        }

        @GetMapping("/server-error")
        public void serverError() {
            throw new RuntimeException("Unexpected failure");
        }

        @PostMapping("/validation")
        public void validation(@Valid @RequestBody ValidationBody body) {
        }

        record ValidationBody(@NotBlank String name) {}
    }

    @Test
    @WithMockUser
    @DisplayName("should return 404 for ResourceNotFoundException")
    void handleNotFound_returns404() throws Exception {
        mockMvc.perform(get("/test-ex/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Resource not found"));
    }

    @Test
    @WithMockUser
    @DisplayName("should return 404 for ComplianceNotFoundException")
    void handleComplianceNotFound_returns404() throws Exception {
        mockMvc.perform(get("/test-ex/compliance-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser
    @DisplayName("should return 400 for InvalidDataException")
    void handleInvalidData_returns400() throws Exception {
        mockMvc.perform(get("/test-ex/invalid-data"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid input provided"));
    }

    @Test
    @WithMockUser
    @DisplayName("should return 500 for unhandled Exception")
    void handleGeneric_returns500() throws Exception {
        mockMvc.perform(get("/test-ex/server-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }

    @Test
    @WithMockUser
    @DisplayName("should return 400 with fieldErrors for @Valid failure")
    void handleValidation_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/test-ex/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }
}
