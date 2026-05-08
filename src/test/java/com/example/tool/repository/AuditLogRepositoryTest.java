package com.example.tool.repository;

import com.example.tool.config.JpaAuditingConfig;
import com.example.tool.entity.AuditLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class AuditLogRepositoryTest {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();

        AuditLog log1 = new AuditLog();
        log1.setEntityType("Compliance");
        log1.setEntityId(1L);
        log1.setAction("CREATE");
        log1.setOldValue(null);
        log1.setNewValue("{\"title\":\"GDPR Audit\"}");
        auditLogRepository.save(log1);

        AuditLog log2 = new AuditLog();
        log2.setEntityType("Compliance");
        log2.setEntityId(1L);
        log2.setAction("UPDATE");
        log2.setOldValue("{\"status\":\"PENDING\"}");
        log2.setNewValue("{\"status\":\"COMPLETED\"}");
        auditLogRepository.save(log2);

        AuditLog log3 = new AuditLog();
        log3.setEntityType("Compliance");
        log3.setEntityId(2L);
        log3.setAction("CREATE");
        log3.setNewValue("{\"title\":\"ISO 27001\"}");
        auditLogRepository.save(log3);
    }

    @Test
    @DisplayName("should save audit log and assign generated id")
    void save_persistsAuditLogWithId() {
        AuditLog log = new AuditLog();
        log.setEntityType("User");
        log.setEntityId(10L);
        log.setAction("DELETE");
        log.setNewValue(null);

        AuditLog saved = auditLogRepository.save(log);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertEquals("User", saved.getEntityType());
    }

    @Test
    @DisplayName("findByEntityTypeAndEntityId should return all logs for given entity")
    void findByEntityTypeAndEntityId_returnsMatchingLogs() {
        List<AuditLog> logs = auditLogRepository.findByEntityTypeAndEntityId("Compliance", 1L);
        assertEquals(2, logs.size());
        assertTrue(logs.stream().allMatch(l -> l.getEntityId().equals(1L)));
    }

    @Test
    @DisplayName("findByEntityTypeAndEntityId should return empty for unknown entity")
    void findByEntityTypeAndEntityId_noMatch_returnsEmpty() {
        List<AuditLog> logs = auditLogRepository.findByEntityTypeAndEntityId("User", 99L);
        assertTrue(logs.isEmpty());
    }

    @Test
    @DisplayName("findByEntityTypeAndEntityId should not return logs for different entityId")
    void findByEntityTypeAndEntityId_differentId_returnsCorrectSubset() {
        List<AuditLog> logs = auditLogRepository.findByEntityTypeAndEntityId("Compliance", 2L);
        assertEquals(1, logs.size());
        assertEquals("CREATE", logs.get(0).getAction());
    }
}
