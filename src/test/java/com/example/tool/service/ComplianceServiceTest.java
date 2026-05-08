package com.example.tool.service;

import com.example.tool.dto.ComplianceRequest;
import com.example.tool.entity.Compliance;
import com.example.tool.exception.InvalidDataException;
import com.example.tool.exception.ResourceNotFoundException;
import com.example.tool.repository.ComplianceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplianceServiceTest {

    @Mock
    private ComplianceRepository complianceRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private ComplianceService complianceService;

    private Compliance compliance;
    private ComplianceRequest request;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(complianceService, "notificationRecipient", "admin@example.com");

        compliance = new Compliance();
        compliance.setId(1L);
        compliance.setTitle("GDPR Compliance");
        compliance.setDescription("Annual GDPR review");
        compliance.setStatus("PENDING");
        compliance.setDueDate(LocalDate.now().plusDays(10));
        compliance.setDeleted(false);
        compliance.setCreatedAt(LocalDateTime.now());
        compliance.setUpdatedAt(LocalDateTime.now());

        request = new ComplianceRequest();
        request.setTitle("GDPR Compliance");
        request.setDescription("Annual GDPR review");
        request.setStatus("PENDING");
        request.setDueDate(LocalDate.now().plusDays(10));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // createRecord()
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("createRecord()")
    class CreateRecord {

        @Test
        @DisplayName("should create record successfully")
        void createRecord_success() {
            when(complianceRepository.save(any(Compliance.class))).thenReturn(compliance);
            doNothing().when(emailService).sendComplianceCreatedEmail(anyString(), any(Compliance.class));

            Compliance result = complianceService.createRecord(request);

            assertNotNull(result);
            assertEquals("GDPR Compliance", result.getTitle());
            assertEquals("PENDING", result.getStatus());
            verify(complianceRepository, times(1)).save(any(Compliance.class));
            verify(emailService, times(1)).sendComplianceCreatedEmail(anyString(), any(Compliance.class));
        }

        @Test
        @DisplayName("should throw InvalidDataException when title is empty")
        void createRecord_emptyTitle_throwsInvalidDataException() {
            request.setTitle("");

            InvalidDataException ex = assertThrows(InvalidDataException.class,
                    () -> complianceService.createRecord(request));

            assertEquals("Title must not be empty", ex.getMessage());
            verify(complianceRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw InvalidDataException when title is blank")
        void createRecord_blankTitle_throwsInvalidDataException() {
            request.setTitle("   ");

            assertThrows(InvalidDataException.class, () -> complianceService.createRecord(request));
            verify(complianceRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw InvalidDataException when due date is in the past")
        void createRecord_pastDueDate_throwsInvalidDataException() {
            request.setDueDate(LocalDate.now().minusDays(1));

            InvalidDataException ex = assertThrows(InvalidDataException.class,
                    () -> complianceService.createRecord(request));

            assertEquals("Due date must not be in the past", ex.getMessage());
            verify(complianceRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getRecordById()
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getRecordById()")
    class GetRecordById {

        @Test
        @DisplayName("should return record when found")
        void getRecordById_found() {
            when(complianceRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(compliance));

            Compliance result = complianceService.getRecordById(1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals("GDPR Compliance", result.getTitle());
            verify(complianceRepository, times(1)).findByIdAndIsDeletedFalse(1L);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when record not found")
        void getRecordById_notFound_throwsResourceNotFoundException() {
            when(complianceRepository.findByIdAndIsDeletedFalse(99L)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> complianceService.getRecordById(99L));

            assertEquals("Compliance record not found with id: 99", ex.getMessage());
            verify(complianceRepository, times(1)).findByIdAndIsDeletedFalse(99L);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getAllRecords()
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getAllRecords()")
    class GetAllRecords {

        @Test
        @DisplayName("should return paginated list of records")
        void getAllRecords_returnsPaginatedList() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Compliance> page = new PageImpl<>(List.of(compliance), pageable, 1);
            when(complianceRepository.findByIsDeletedFalse(pageable)).thenReturn(page);

            Page<Compliance> result = complianceService.getAllRecords(pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            assertEquals(1, result.getContent().size());
            assertEquals("GDPR Compliance", result.getContent().get(0).getTitle());
            verify(complianceRepository, times(1)).findByIsDeletedFalse(pageable);
        }

        @Test
        @DisplayName("should return empty page when no records exist")
        void getAllRecords_emptyPage() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Compliance> emptyPage = new PageImpl<>(List.of(), pageable, 0);
            when(complianceRepository.findByIsDeletedFalse(pageable)).thenReturn(emptyPage);

            Page<Compliance> result = complianceService.getAllRecords(pageable);

            assertNotNull(result);
            assertEquals(0, result.getTotalElements());
            assertTrue(result.getContent().isEmpty());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // updateRecord()
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("updateRecord()")
    class UpdateRecord {

        @Test
        @DisplayName("should update record successfully")
        void updateRecord_success() {
            request.setTitle("Updated Title");
            request.setStatus("COMPLETED");

            when(complianceRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(compliance));
            when(complianceRepository.save(any(Compliance.class))).thenReturn(compliance);

            Compliance result = complianceService.updateRecord(1L, request);

            assertNotNull(result);
            verify(complianceRepository, times(1)).findByIdAndIsDeletedFalse(1L);
            verify(complianceRepository, times(1)).save(any(Compliance.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when record not found")
        void updateRecord_notFound_throwsResourceNotFoundException() {
            when(complianceRepository.findByIdAndIsDeletedFalse(99L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> complianceService.updateRecord(99L, request));

            verify(complianceRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw InvalidDataException when title is empty on update")
        void updateRecord_emptyTitle_throwsInvalidDataException() {
            request.setTitle("");

            assertThrows(InvalidDataException.class,
                    () -> complianceService.updateRecord(1L, request));

            verify(complianceRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw InvalidDataException when due date is past on update")
        void updateRecord_pastDueDate_throwsInvalidDataException() {
            request.setDueDate(LocalDate.now().minusDays(5));

            assertThrows(InvalidDataException.class,
                    () -> complianceService.updateRecord(1L, request));

            verify(complianceRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // deleteRecord()
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("deleteRecord()")
    class DeleteRecord {

        @Test
        @DisplayName("should soft delete record successfully")
        void deleteRecord_success() {
            when(complianceRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(compliance));
            when(complianceRepository.save(any(Compliance.class))).thenReturn(compliance);

            complianceService.deleteRecord(1L);

            assertTrue(compliance.isDeleted());
            verify(complianceRepository, times(1)).findByIdAndIsDeletedFalse(1L);
            verify(complianceRepository, times(1)).save(compliance);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when record not found")
        void deleteRecord_notFound_throwsResourceNotFoundException() {
            when(complianceRepository.findByIdAndIsDeletedFalse(99L)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> complianceService.deleteRecord(99L));

            assertEquals("Compliance record not found with id: 99", ex.getMessage());
            verify(complianceRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getStats()
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getStats()")
    class GetStats {

        @Test
        @DisplayName("should return correct stats map")
        void getStats_returnsCorrectStats() {
            when(complianceRepository.countByIsDeletedFalse()).thenReturn(10L);
            when(complianceRepository.countByStatusAndIsDeletedFalse("PENDING")).thenReturn(4L);
            when(complianceRepository.countByStatusAndIsDeletedFalse("COMPLETED")).thenReturn(3L);
            when(complianceRepository.countByStatusAndIsDeletedFalse("OVERDUE")).thenReturn(2L);
            when(complianceRepository.countByStatusAndIsDeletedFalse("OPEN")).thenReturn(1L);
            when(complianceRepository.countByStatusAndIsDeletedFalse("CLOSED")).thenReturn(0L);

            var stats = complianceService.getStats();

            assertEquals(10L, stats.get("total"));
            assertEquals(4L,  stats.get("pending"));
            assertEquals(3L,  stats.get("completed"));
            assertEquals(2L,  stats.get("overdue"));
            assertEquals(1L,  stats.get("open"));
            assertEquals(0L,  stats.get("closed"));
        }
    }
}
