package com.example.tool.service;

import com.example.tool.dto.ComplianceRequest;
import com.example.tool.entity.Compliance;
import com.example.tool.exception.InvalidDataException;
import com.example.tool.exception.ResourceNotFoundException;
import com.example.tool.repository.ComplianceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComplianceService {

    private final ComplianceRepository complianceRepository;
    private final EmailService emailService;

    @Value("${notification.email.recipient:admin@example.com}")
    private String notificationRecipient;

    @Caching(evict = {
            @CacheEvict(value = "complianceRecords", allEntries = true),
            @CacheEvict(value = "complianceById",    allEntries = true)
    })
    public Compliance createRecord(ComplianceRequest request) {
        validate(request);
        Compliance c = new Compliance();
        c.setTitle(request.getTitle());
        c.setDescription(request.getDescription());
        c.setStatus(request.getStatus());
        c.setDueDate(request.getDueDate());
        Compliance saved = complianceRepository.save(c);
        log.info("Compliance record created with id: {}", saved.getId());
        emailService.sendComplianceCreatedEmail(notificationRecipient, saved);
        return saved;
    }

    @Cacheable(value = "complianceRecords",
            key = "#pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort",
            unless = "#result == null")
    public Page<Compliance> getAllRecords(Pageable pageable) {
        log.info("Cache MISS - fetching complianceRecords from DB for page: {}", pageable.getPageNumber());
        return complianceRepository.findByIsDeletedFalse(pageable);
    }

    @Cacheable(value = "complianceById", key = "#id", unless = "#result == null")
    public Compliance getRecordById(Long id) {
        log.info("Cache MISS - fetching complianceById from DB for id: {}", id);
        return complianceRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compliance record not found with id: " + id));
    }

    @Caching(evict = {
            @CacheEvict(value = "complianceRecords", allEntries = true),
            @CacheEvict(value = "complianceById",    key = "#id")
    })
    public Compliance updateRecord(Long id, ComplianceRequest request) {
        validate(request);
        Compliance c = complianceRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compliance record not found with id: " + id));
        c.setTitle(request.getTitle());
        c.setDescription(request.getDescription());
        c.setStatus(request.getStatus());
        c.setDueDate(request.getDueDate());
        Compliance updated = complianceRepository.save(c);
        log.info("Compliance record updated with id: {}", id);
        return updated;
    }

    @Caching(evict = {
            @CacheEvict(value = "complianceRecords", allEntries = true),
            @CacheEvict(value = "complianceById",    key = "#id")
    })
    public void deleteRecord(Long id) {
        Compliance c = complianceRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compliance record not found with id: " + id));
        c.setDeleted(true);
        complianceRepository.save(c);
        log.info("Compliance record soft-deleted with id: {}", id);
    }

    public List<Compliance> search(String keyword) {
        return complianceRepository.search(keyword);
    }

    public Map<String, Long> getStats() {
        return Map.of(
                "total",     complianceRepository.countByIsDeletedFalse(),
                "pending",   complianceRepository.countByStatusAndIsDeletedFalse("PENDING"),
                "completed", complianceRepository.countByStatusAndIsDeletedFalse("COMPLETED"),
                "overdue",   complianceRepository.countByStatusAndIsDeletedFalse("OVERDUE"),
                "open",      complianceRepository.countByStatusAndIsDeletedFalse("OPEN"),
                "closed",    complianceRepository.countByStatusAndIsDeletedFalse("CLOSED")
        );
    }

    private void validate(ComplianceRequest r) {
        if (r.getTitle() == null || r.getTitle().isBlank())
            throw new InvalidDataException("Title must not be empty");
        if (r.getDueDate() != null && r.getDueDate().isBefore(LocalDate.now()))
            throw new InvalidDataException("Due date must not be in the past");
    }
}
