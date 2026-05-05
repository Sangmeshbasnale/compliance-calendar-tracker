package com.example.tool.aspect;

import com.example.tool.dto.ComplianceRequest;
import com.example.tool.entity.AuditLog;
import com.example.tool.entity.Compliance;
import com.example.tool.repository.AuditLogRepository;
import com.example.tool.repository.ComplianceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * AOP aspect that intercepts CREATE, UPDATE, DELETE operations on
 * ComplianceService and persists an AuditLog entry for each.
 *
 * Captures: entity_type, entity_id, action, old_value (JSON),
 *           new_value (JSON), performed_by, timestamp.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLoggingAspect {

    private final AuditLogRepository  auditLogRepository;
    private final ComplianceRepository complianceRepository;
    private final ObjectMapper         objectMapper;

    // ── CREATE ────────────────────────────────────────────────────

    @Around("execution(* com.example.tool.service.ComplianceService.create(..))")
    public Object logCreate(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();

        if (result instanceof Compliance compliance) {
            saveAuditLog("Compliance", compliance.getId(), "CREATE",
                    null, toJson(compliance));
        }
        return result;
    }

    // ── UPDATE ────────────────────────────────────────────────────

    @Around("execution(* com.example.tool.service.ComplianceService.update(..)) && args(id, request)")
    public Object logUpdate(ProceedingJoinPoint joinPoint,
                            Long id, ComplianceRequest request) throws Throwable {
        String oldJson = toJson(complianceRepository.findById(id).orElse(null));
        Object result  = joinPoint.proceed();

        if (result instanceof Compliance compliance) {
            saveAuditLog("Compliance", compliance.getId(), "UPDATE",
                    oldJson, toJson(compliance));
        }
        return result;
    }

    // ── DELETE ────────────────────────────────────────────────────

    @Around("execution(* com.example.tool.service.ComplianceService.delete(..)) && args(id)")
    public Object logDelete(ProceedingJoinPoint joinPoint, Long id) throws Throwable {
        String oldJson = toJson(complianceRepository.findById(id).orElse(null));
        Object result  = joinPoint.proceed();
        saveAuditLog("Compliance", id, "DELETE", oldJson, null);
        return result;
    }

    // ── Helpers ───────────────────────────────────────────────────

    private void saveAuditLog(String entityType, Long entityId, String action,
                               String oldValue, String newValue) {
        try {
            AuditLog log = new AuditLog();
            log.setEntityType(entityType);
            log.setEntityId(entityId);
            log.setAction(action);
            log.setOldValue(oldValue);
            log.setNewValue(newValue);
            log.setPerformedBy(currentUsername());
            auditLogRepository.save(log);
        } catch (RuntimeException e) {
            // Audit failure must never break the main operation
            this.log.error("Audit log failed [{} {}]: {}", action, entityId, e.getMessage(), e);
        }
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            this.log.warn("Failed to serialize object to JSON: {}", e.getMessage(), e);
            return null;
        }
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "anonymous";
    }
}
