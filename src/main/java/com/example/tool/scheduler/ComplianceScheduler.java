package com.example.tool.scheduler;

import com.example.tool.entity.Compliance;
import com.example.tool.repository.ComplianceRepository;
import com.example.tool.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ComplianceScheduler {

    private static final LocalDate EPOCH_START = LocalDate.of(2000, 1, 1);

    private final ComplianceRepository complianceRepository;
    private final EmailService emailService;

    @Value("${notification.email.recipient:admin@example.com}")
    private String notificationRecipient;

    @Scheduled(cron = "0 0 0 * * *")
    public void markOverdueRecords() {
        List<Compliance> overdue = complianceRepository
                .findByIsDeletedFalseAndDueDateBetween(EPOCH_START, LocalDate.now().minusDays(1))
                .stream()
                .filter(c -> "PENDING".equals(c.getStatus()))
                .toList();

        overdue.forEach(c -> c.setStatus("OVERDUE"));
        complianceRepository.saveAll(overdue);
        log.info("Marked {} compliance records as OVERDUE", overdue.size());

        overdue.forEach(c -> emailService.sendOverdueEmail(notificationRecipient, c));
    }
}
