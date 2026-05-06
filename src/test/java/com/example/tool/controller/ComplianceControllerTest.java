package com.example.tool.controller;

import com.example.tool.dto.ComplianceRequest;
import com.example.tool.dto.ComplianceResponse;
import com.example.tool.entity.Compliance;
import com.example.tool.service.ComplianceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice test for ComplianceController — security filters disabled,
 * ComplianceService fully mocked.
 */
@WebMvcTest(ComplianceController.class)
@AutoConfigureMockMvc(addFilters = false)
class ComplianceControllerTest {

    @Autowired MockMvc       mockMvc;
    @Autowired ObjectMapper  objectMapper;

    @MockBean ComplianceService complianceService;

    private Compliance        compliance;
    private ComplianceRequest request;

    @BeforeEach
    void setUp() {
        compliance = new Compliance();
        compliance.setId(1L);
        compliance.setTitle("Test Title");
        compliance.setDescription("Test Description");
        compliance.setStatus("PENDING");
        compliance.setDueDate(LocalDate.now());
        compliance.setCreatedAt(LocalDateTime.now());
        compliance.setUpdatedAt(LocalDateTime.now());

        request = new ComplianceRequest();
        request.setTitle("Test Title");
        request.setDescription("Test Description");
        request.setStatus("PENDING");
        request.setDueDate(LocalDate.now());
    }

    @Test
    void testGetAll() throws Exception {
        Page<Compliance> page = new PageImpl<>(List.of(compliance));
        Mockito.when(complianceService.getAll(anyInt(), anyInt(), anyString(), anyString()))
               .thenReturn(page);

        mockMvc.perform(get("/api/compliance")
                        .param("page", "0").param("size", "10")
                        .param("sortBy", "id").param("sortDir", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title", is("Test Title")));
    }

    @Test
    void testCreate() throws Exception {
        Mockito.when(complianceService.create(org.mockito.ArgumentMatchers.any(ComplianceRequest.class))).thenReturn(compliance);

        mockMvc.perform(post("/api/compliance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("Test Title")));
    }

    @Test
    void testUpdate() throws Exception {
        Mockito.when(complianceService.update(eq(1L), org.mockito.ArgumentMatchers.any(ComplianceRequest.class)))
               .thenReturn(compliance);

        mockMvc.perform(put("/api/compliance/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Test Title")));
    }

    @Test
    void testDelete() throws Exception {
        Mockito.doNothing().when(complianceService).delete(1L);

        mockMvc.perform(delete("/api/compliance/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void testSearch() throws Exception {
        Page<Compliance> page = new PageImpl<>(List.of(compliance));
        Mockito.when(complianceService.search(anyString(), anyInt(), anyInt(), anyString(), anyString()))
               .thenReturn(page);

        mockMvc.perform(get("/api/compliance/search").param("q", "Test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title", is("Test Title")));
    }

    @Test
    void testStats() throws Exception {
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", 1L);
        Mockito.when(complianceService.getStats()).thenReturn(stats);

        mockMvc.perform(get("/api/compliance/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", is(1)));
    }

    @Test
    void testExportToCsv() throws Exception {
        Mockito.when(complianceService.exportAll()).thenReturn(List.of(compliance));

        mockMvc.perform(get("/api/compliance/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"))
                .andExpect(content().string(containsString("ID,Title")))
                .andExpect(content().string(containsString("Test Title")));
    }

    @Test
    void testCreateValidationFailure() throws Exception {
        // Missing required title
        String badBody = """
                { "status": "PENDING", "dueDate": "2025-12-31" }
                """;

        mockMvc.perform(post("/api/compliance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title", notNullValue()));
    }
}
