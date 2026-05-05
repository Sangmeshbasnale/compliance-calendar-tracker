package com.example.tool.service;

import com.example.tool.entity.Compliance;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Async
    public void sendEmail(String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);
            mailSender.send(message);
            log.info("Email sent to: {} | subject: {}", to, subject);
        } catch (MessagingException e) {
            log.error("Failed to send email to: {}", to, e);
        }
    }

    @Async
    public void sendComplianceCreatedEmail(String to, Compliance compliance) {
        try {
            Context ctx = new Context();
            ctx.setVariable("title",       compliance.getTitle());
            ctx.setVariable("description", compliance.getDescription());
            ctx.setVariable("status",      compliance.getStatus());
            ctx.setVariable("dueDate",     compliance.getDueDate());
            String content = templateEngine.process("emails/compliance-created", ctx);
            sendEmail(to, "New Compliance Record Created: " + compliance.getTitle(), content);
        } catch (MessagingException e) {
            log.error("Failed to send compliance-created email for id: {}", compliance.getId(), e);
        }
    }

    @Async
    public void sendOverdueEmail(String to, Compliance compliance) {
        try {
            Context ctx = new Context();
            ctx.setVariable("title",   compliance.getTitle());
            ctx.setVariable("dueDate", compliance.getDueDate());
            ctx.setVariable("status",  compliance.getStatus());
            String content = templateEngine.process("emails/compliance-overdue", ctx);
            sendEmail(to, "Overdue Compliance Alert: " + compliance.getTitle(), content);
        } catch (MessagingException e) {
            log.error("Failed to send overdue email for id: {}", compliance.getId(), e);
        }
    }
}
