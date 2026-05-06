package com.example.tool.service;

import com.example.tool.dto.ComplianceRequest;
import com.example.tool.entity.Compliance;
import com.example.tool.exception.ComplianceNotFoundException;
import com.example.tool.repository.ComplianceRepository;
import com.example.tool.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ComplianceService {

    private final ComplianceRepository complianceRepository;
    private final EmailService emailService;
    private final UserRepository userRepository;

    public Page<Compliance> getAll(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending().and(Sort.by("id").ascending())
                : Sort.by(sortBy).descending().and(Sort.by("id").ascending());
        return complianceRepository.findAll(PageRequest.of(page, size, sort));
    }

    public List<Compliance> exportAll() {
        return complianceRepository.findAll();
    }

    public Compliance getById(Long id) {
        return complianceRepository.findById(id)
                .orElseThrow(() -> new ComplianceNotFoundException(id));
    }

    public Compliance create(ComplianceRequest request) {
        Compliance compliance = new Compliance();
        compliance.setTitle(request.getTitle());
        compliance.setDescription(request.getDescription());
        compliance.setStatus(request.getStatus());
        compliance.setDueDate(request.getDueDate());
        Compliance saved = complianceRepository.save(compliance);

        // Notify the currently authenticated user via email (if they have one)
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        userRepository.findByUsername(username)
                .filter(u -> u.getEmail() != null && !u.getEmail().isBlank())
                .ifPresent(u -> emailService.sendCreationNotification(u.getEmail(), saved));

        return saved;
    }

    public Compliance update(Long id, ComplianceRequest request) {
        Compliance existing = getById(id);
        existing.setTitle(request.getTitle());
        existing.setDescription(request.getDescription());
        existing.setStatus(request.getStatus());
        existing.setDueDate(request.getDueDate());
        return complianceRepository.save(existing);
    }

    public void delete(Long id) {
        if (!complianceRepository.existsById(id)) {
            throw new ComplianceNotFoundException(id);
        }
        complianceRepository.deleteById(id);
    }

    public Page<Compliance> search(String keyword, int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending().and(Sort.by("id").ascending())
                : Sort.by(sortBy).descending().and(Sort.by("id").ascending());
        return complianceRepository.searchByTitle(keyword, PageRequest.of(page, size, sort));
    }

    public Map<String, Long> getStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", complianceRepository.count());
        stats.put("pending", complianceRepository.countByStatus("PENDING"));
        stats.put("completed", complianceRepository.countByStatus("COMPLETED"));
        stats.put("overdue", complianceRepository.countByStatus("OVERDUE"));
        return stats;
    }
}
