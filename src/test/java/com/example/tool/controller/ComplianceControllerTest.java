package com.example.tool.controller;

import com.example.tool.config.JwtAuthFilter;
import com.example.tool.config.JwtUtil;
import com.example.tool.dto.ComplianceRequest;
import com.example.tool.entity.Compliance;
import com.example.tool.exception.InvalidDataException;
import com.example.tool.exception.ResourceNotFoundException;
import com.example.tool.service.ComplianceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ComplianceController.class)
class ComplianceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ComplianceService complianceService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private Compliance compliance;
    private ComplianceRequest request;

    @BeforeEach
    void setUp() {
        compliance = new Compliance();
        compliance.setId(1L);
        compliance.setTitle("GDPR Audit");
        compliance.setDescription("Annual GDPR review");
        compliance.setStatus("PENDING");
        compliance.setDueDate(LocalDate.now().plusDays(10));
        compliance.setDeleted(false);
        compliance.setCreatedAt(LocalDateTime.now());
        compliance.setUpdatedAt(LocalDateTime.now());

        request = new ComplianceRequest();
        request.setTitle("GDPR Audit");
        request.setDescription("Annual GDPR review");
        request.setStatus("PENDING");
        request.setDueDate(LocalDate.now().plusDays(10));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/compliance
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /api/compliance")
    class GetAll {

        @Test
        @WithMockUser(authorities = "ROLE_VIEWER")
        @DisplayName("should return 200 with paginated compliance list")
        void getAll_returns200WithPage() throws Exception {
            Pageable pageable = PageRequest.of(0, 10);
            when(complianceService.getAllRecords(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(compliance), pageable, 1));

            mockMvc.perform(get("/api/compliance"))
                    .andExpect(status().isOk());

            verify(complianceService).getAllRecords(any(Pageable.class));
        }

        @Test
        @WithMockUser(authorities = "ROLE_VIEWER")
        @DisplayName("should return 200 with empty page when no records")
        void getAll_emptyPage_returns200() throws Exception {
            when(complianceService.getAllRecords(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/api/compliance"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 401 when not authenticated")
        void getAll_unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/api/compliance"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/compliance/{id}
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /api/compliance/{id}")
    class GetById {

        @Test
        @WithMockUser(authorities = "ROLE_VIEWER")
        @DisplayName("should return 200 with compliance record")
        void getById_found_returns200() throws Exception {
            when(complianceService.getRecordById(1L)).thenReturn(compliance);

            mockMvc.perform(get("/api/compliance/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.title").value("GDPR Audit"))
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @WithMockUser(authorities = "ROLE_VIEWER")
        @DisplayName("should return 404 when record not found")
        void getById_notFound_returns404() throws Exception {
            when(complianceService.getRecordById(99L))
                    .thenThrow(new ResourceNotFoundException("Compliance record not found with id: 99"));

            mockMvc.perform(get("/api/compliance/99"))
                    .andExpect(status().isNotFound());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/compliance
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("POST /api/compliance")
    class Create {

        @Test
        @WithMockUser(authorities = "ROLE_ADMIN")
        @DisplayName("should return 201 on successful creation")
        void create_success_returns201() throws Exception {
            when(complianceService.createRecord(any(ComplianceRequest.class))).thenReturn(compliance);

            mockMvc.perform(post("/api/compliance")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title").value("GDPR Audit"))
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @WithMockUser(authorities = "ROLE_ADMIN")
        @DisplayName("should return 400 when title is missing")
        void create_missingTitle_returns400() throws Exception {
            request.setTitle("");

            mockMvc.perform(post("/api/compliance")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(authorities = "ROLE_ADMIN")
        @DisplayName("should return 400 when service throws InvalidDataException")
        void create_invalidData_returns400() throws Exception {
            when(complianceService.createRecord(any(ComplianceRequest.class)))
                    .thenThrow(new InvalidDataException("Due date must not be in the past"));

            mockMvc.perform(post("/api/compliance")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(authorities = "ROLE_VIEWER")
        @DisplayName("should return 403 when viewer tries to create")
        void create_viewer_returns403() throws Exception {
            mockMvc.perform(post("/api/compliance")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /api/compliance/{id}
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("PUT /api/compliance/{id}")
    class Update {

        @Test
        @WithMockUser(authorities = "ROLE_ADMIN")
        @DisplayName("should return 200 on successful update")
        void update_success_returns200() throws Exception {
            when(complianceService.updateRecord(anyLong(), any(ComplianceRequest.class)))
                    .thenReturn(compliance);

            mockMvc.perform(put("/api/compliance/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));
        }

        @Test
        @WithMockUser(authorities = "ROLE_ADMIN")
        @DisplayName("should return 404 when record not found on update")
        void update_notFound_returns404() throws Exception {
            when(complianceService.updateRecord(anyLong(), any(ComplianceRequest.class)))
                    .thenThrow(new ResourceNotFoundException("Compliance record not found with id: 99"));

            mockMvc.perform(put("/api/compliance/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE /api/compliance/{id}
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("DELETE /api/compliance/{id}")
    class Delete {

        @Test
        @WithMockUser(authorities = "ROLE_ADMIN")
        @DisplayName("should return 204 on successful soft delete")
        void delete_success_returns204() throws Exception {
            doNothing().when(complianceService).deleteRecord(1L);

            mockMvc.perform(delete("/api/compliance/1"))
                    .andExpect(status().isNoContent());

            verify(complianceService).deleteRecord(1L);
        }

        @Test
        @WithMockUser(authorities = "ROLE_ADMIN")
        @DisplayName("should return 404 when record not found on delete")
        void delete_notFound_returns404() throws Exception {
            doThrow(new ResourceNotFoundException("Compliance record not found with id: 99"))
                    .when(complianceService).deleteRecord(99L);

            mockMvc.perform(delete("/api/compliance/99"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(authorities = "ROLE_VIEWER")
        @DisplayName("should return 403 when viewer tries to delete")
        void delete_viewer_returns403() throws Exception {
            mockMvc.perform(delete("/api/compliance/1"))
                    .andExpect(status().isForbidden());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/compliance/search
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /api/compliance/search")
    class Search {

        @Test
        @WithMockUser(authorities = "ROLE_VIEWER")
        @DisplayName("should return 200 with matching results")
        void search_returnsResults() throws Exception {
            when(complianceService.search("gdpr")).thenReturn(List.of(compliance));

            mockMvc.perform(get("/api/compliance/search").param("q", "gdpr"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].title").value("GDPR Audit"));
        }

        @Test
        @WithMockUser(authorities = "ROLE_VIEWER")
        @DisplayName("should return 200 with empty list when no match")
        void search_noMatch_returnsEmptyList() throws Exception {
            when(complianceService.search(anyString())).thenReturn(List.of());

            mockMvc.perform(get("/api/compliance/search").param("q", "xyz"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/compliance/stats
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /api/compliance/stats")
    class Stats {

        @Test
        @WithMockUser(authorities = "ROLE_VIEWER")
        @DisplayName("should return 200 with stats map")
        void stats_returns200WithMap() throws Exception {
            when(complianceService.getStats()).thenReturn(Map.of(
                    "total", 5L, "pending", 2L, "completed", 1L,
                    "overdue", 1L, "open", 1L, "closed", 0L));

            mockMvc.perform(get("/api/compliance/stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(5))
                    .andExpect(jsonPath("$.pending").value(2));
        }
    }
}
