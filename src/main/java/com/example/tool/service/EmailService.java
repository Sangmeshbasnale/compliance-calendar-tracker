package com.example.tool.service;

import com.example.tool.entity.Compliance;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sends HTML emails using JavaMailSender + Thymeleaf templates.
 * All send methods are @Async so they never block the calling thread.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Set<String> ALLOWED_TEMPLATES = Set.of(
            "email/compliance-created",
            "email/compliance-overdue",
            "email/weekly-summary"
    );

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.mail.admin-email}")
    private String adminEmail;

    // ── Public API ────────────────────────────────────────────────

    /**
     * Notify a recipient that a new compliance record was created.
     *
     * @param to         recipient email address
     * @param compliance the newly created record
     */
    @Async
    public void sendCreationNotification(String to, Compliance compliance) {
        Context ctx = new Context();
        ctx.setVariables(Map.of(
                "title",       compliance.getTitle(),
                "status",      compliance.getStatus(),
                "dueDate",     compliance.getDueDate(),
                "description", compliance.getDescription() != null ? compliance.getDescription() : ""
        ));
        sendHtml(to, "New Compliance Record: " + compliance.getTitle(),
                "email/compliance-created", ctx);
    }

    /**
     * Notify a recipient that a compliance record is overdue.
     *
     * @param to         recipient email address
     * @param compliance the overdue record
     */
    @Async
    public void sendOverdueNotification(String to, Compliance compliance) {
        Context ctx = new Context();
        ctx.setVariables(Map.of(
                "title",   compliance.getTitle(),
                "dueDate", compliance.getDueDate(),
                "status",  compliance.getStatus()
        ));
        sendHtml(to, "OVERDUE: " + compliance.getTitle(), "email/compliance-overdue", ctx);
    }

    /**
     * Send the weekly summary report to the admin email.
     *
     * @param items all compliance items for the summary
     */
    @Async
    public void sendWeeklySummary(List<Compliance> items) {
        long pending   = items.stream().filter(c -> "PENDING".equals(c.getStatus())).count();
        long completed = items.stream().filter(c -> "COMPLETED".equals(c.getStatus())).count();
        long overdue   = items.stream().filter(c -> "OVERDUE".equals(c.getStatus())).count();

        Context ctx = new Context();
        ctx.setVariables(Map.of(
                "total",     items.size(),
                "pending",   pending,
                "completed", completed,
                "overdue",   overdue,
                "items",     items
        ));
        sendHtml(adminEmail, "Weekly Compliance Summary", "email/weekly-summary", ctx);
    }

    // ── Internal helper ───────────────────────────────────────────

    private void sendHtml(String to, String subject, String template, Context ctx) {
        // Validate template name against known safe templates to prevent path traversal
        if (!ALLOWED_TEMPLATES.contains(template)) {
            log.error("Rejected unknown email template: {}", template);
            return;
        }
        try {
            String html = templateEngine.process(template, ctx);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Email sent to {} | subject: {}", to, subject);
        } catch (MessagingException e) {
            log.error("Failed to send email to {} | subject: {} | error: {}", to, subject, e.getMessage(), e);
        }
    }
}
