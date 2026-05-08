package com.example.tool.repository;

import com.example.tool.config.JpaAuditingConfig;
import com.example.tool.entity.Compliance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class ComplianceRepositoryTest {

    @Autowired
    private ComplianceRepository complianceRepository;

    private Compliance active;
    private Compliance softDeleted;

    @BeforeEach
    void setUp() {
        complianceRepository.deleteAll();

        active = new Compliance();
        active.setTitle("GDPR Audit");
        active.setDescription("Annual GDPR review process");
        active.setStatus("PENDING");
        active.setDueDate(LocalDate.now().plusDays(10));
        active.setDeleted(false);
        complianceRepository.save(active);

        softDeleted = new Compliance();
        softDeleted.setTitle("Old Policy");
        softDeleted.setDescription("Deprecated compliance item");
        softDeleted.setStatus("COMPLETED");
        softDeleted.setDueDate(LocalDate.now().plusDays(5));
        softDeleted.setDeleted(true);
        complianceRepository.save(softDeleted);
    }

    @Nested
    @DisplayName("save() and findById()")
    class SaveAndFind {

        @Test
        @DisplayName("should persist and retrieve a record by id")
        void save_andFindById_success() {
            Compliance c = new Compliance();
            c.setTitle("ISO 27001");
            c.setStatus("OPEN");
            c.setDueDate(LocalDate.now().plusDays(30));
            c.setDeleted(false);

            Compliance saved = complianceRepository.save(c);

            assertNotNull(saved.getId());
            Optional<Compliance> found = complianceRepository.findById(saved.getId());
            assertTrue(found.isPresent());
            assertEquals("ISO 27001", found.get().getTitle());
        }

        @Test
        @DisplayName("findById should return empty for non-existent id")
        void findById_notFound_returnsEmpty() {
            assertTrue(complianceRepository.findById(9999L).isEmpty());
        }
    }

    @Nested
    @DisplayName("findByIsDeletedFalse()")
    class FindActive {

        @Test
        @DisplayName("should return only non-deleted records")
        void findByIsDeletedFalse_returnsOnlyActive() {
            Page<Compliance> page = complianceRepository.findByIsDeletedFalse(PageRequest.of(0, 10));
            assertEquals(1, page.getTotalElements());
            assertEquals("GDPR Audit", page.getContent().get(0).getTitle());
        }
    }

    @Nested
    @DisplayName("findByIdAndIsDeletedFalse()")
    class FindActiveById {

        @Test
        @DisplayName("should return record when active")
        void findByIdAndIsDeletedFalse_found() {
            assertTrue(complianceRepository.findByIdAndIsDeletedFalse(active.getId()).isPresent());
        }

        @Test
        @DisplayName("should return empty for soft-deleted record")
        void findByIdAndIsDeletedFalse_deleted_returnsEmpty() {
            assertTrue(complianceRepository.findByIdAndIsDeletedFalse(softDeleted.getId()).isEmpty());
        }
    }

    @Nested
    @DisplayName("search()")
    class Search {

        @Test
        @DisplayName("should match by partial title case-insensitively")
        void search_byTitle_returnsMatch() {
            List<Compliance> results = complianceRepository.search("gdpr");
            assertEquals(1, results.size());
            assertEquals("GDPR Audit", results.get(0).getTitle());
        }

        @Test
        @DisplayName("should match by partial description")
        void search_byDescription_returnsMatch() {
            List<Compliance> results = complianceRepository.search("annual");
            assertEquals(1, results.size());
        }

        @Test
        @DisplayName("should not return soft-deleted records")
        void search_excludesDeleted() {
            assertTrue(complianceRepository.search("deprecated").isEmpty());
        }

        @Test
        @DisplayName("should return empty list when no match")
        void search_noMatch_returnsEmpty() {
            assertTrue(complianceRepository.search("xyz-no-match").isEmpty());
        }
    }

    @Nested
    @DisplayName("countByStatusAndIsDeletedFalse()")
    class CountByStatus {

        @Test
        @DisplayName("should count active records by status correctly")
        void countByStatus_returnsCorrectCount() {
            assertEquals(1, complianceRepository.countByStatusAndIsDeletedFalse("PENDING"));
            assertEquals(0, complianceRepository.countByStatusAndIsDeletedFalse("COMPLETED"));
        }
    }

    @Nested
    @DisplayName("findByIsDeletedFalseAndDueDateBetween()")
    class DateRange {

        @Test
        @DisplayName("should return active records within date range")
        void findByDateRange_returnsMatchingRecords() {
            List<Compliance> results = complianceRepository
                    .findByIsDeletedFalseAndDueDateBetween(LocalDate.now().plusDays(1), LocalDate.now().plusDays(15));
            assertEquals(1, results.size());
            assertEquals("GDPR Audit", results.get(0).getTitle());
        }

        @Test
        @DisplayName("should return empty when no records fall in range")
        void findByDateRange_noMatch_returnsEmpty() {
            List<Compliance> results = complianceRepository
                    .findByIsDeletedFalseAndDueDateBetween(LocalDate.now().plusDays(50), LocalDate.now().plusDays(60));
            assertTrue(results.isEmpty());
        }
    }
}
