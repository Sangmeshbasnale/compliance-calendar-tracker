package com.example.tool.seeder;

import com.example.tool.entity.Compliance;
import com.example.tool.entity.User;
import com.example.tool.repository.ComplianceRepository;
import com.example.tool.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final ComplianceRepository complianceRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedUsers();
        seedCompliance();
    }

    private void seedUsers() {
        if (userRepository.count() > 0) {
            log.info("Users already seeded — skipping.");
            return;
        }
        log.info("Seeding demo users...");
        userRepository.saveAll(List.of(
                buildUser("admin",   "admin123",   "ROLE_ADMIN"),
                buildUser("manager", "manager123", "ROLE_MANAGER"),
                buildUser("viewer",  "viewer123",  "ROLE_VIEWER")
        ));
        log.info("Inserted 3 demo users (admin, manager, viewer) successfully.");
    }

    private User buildUser(String username, String rawPassword, String role) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(rawPassword));
        u.setRole(role);
        return u;
    }

    private void seedCompliance() {
        if (complianceRepository.count() > 0) {
            log.info("Compliance records already seeded — skipping.");
            return;
        }
        log.info("Seeding compliance records...");
        LocalDate today = LocalDate.now();

        List<Compliance> records = List.of(
                build("GST Filing Q1",               "Quarterly GST return filing for Q1",                     "PENDING",   today.plusDays(10)),
                build("GST Filing Q2",               "Quarterly GST return filing for Q2",                     "PENDING",   today.plusDays(40)),
                build("Income Tax Return",           "Annual income tax return submission",                     "PENDING",   today.plusDays(20)),
                build("TDS Payment - March",         "Monthly TDS payment for March",                          "PENDING",   today.plusDays(5)),
                build("PF Contribution Filing",      "Monthly provident fund contribution filing",             "PENDING",   today.plusDays(15)),
                build("ESI Return Filing",           "Employee state insurance return filing",                 "PENDING",   today.plusDays(25)),
                build("ROC Annual Filing",           "Registrar of Companies annual return filing",            "PENDING",   today.plusDays(60)),
                build("FSSAI License Renewal",       "Food safety and standards authority license renewal",    "PENDING",   today.plusDays(90)),
                build("ISO 27001 Audit",             "Annual information security management audit",           "OPEN",      today.plusDays(30)),
                build("GDPR Compliance Review",      "Annual GDPR data processing activities review",         "OPEN",      today.plusDays(45)),
                build("Data Privacy Assessment",     "Internal data privacy impact assessment",               "OPEN",      today.plusDays(35)),
                build("Cybersecurity Policy Review", "Review and update cybersecurity policies",              "OPEN",      today.plusDays(50)),
                build("Fire Safety Inspection",      "Annual fire safety equipment inspection",               "OPEN",      today.plusDays(55)),
                build("Environmental Compliance",    "Environmental impact compliance check",                 "OPEN",      today.plusDays(70)),
                build("Trade License Renewal",       "Annual trade license renewal with municipal authority", "OPEN",      today.plusDays(80)),
                build("Q3 GST Filing",               "Quarterly GST return filing for Q3",                    "COMPLETED", today.minusDays(10)),
                build("Annual HR Policy Review",     "Yearly HR policy and handbook review",                  "COMPLETED", today.minusDays(20)),
                build("Payroll Audit",               "Internal payroll audit for FY 2024",                    "COMPLETED", today.minusDays(30)),
                build("Vendor Contract Review",      "Annual vendor contract compliance review",              "COMPLETED", today.minusDays(15)),
                build("Software License Audit",      "Audit of all software licenses in use",                 "COMPLETED", today.minusDays(25)),
                build("POSH Training",               "Prevention of sexual harassment training completion",   "COMPLETED", today.minusDays(40)),
                build("Board Meeting Minutes",       "Filing of board meeting minutes with ROC",              "COMPLETED", today.minusDays(5)),
                build("Statutory Audit FY2024",      "Annual statutory audit for financial year 2024",        "COMPLETED", today.minusDays(60)),
                build("TDS Return Q4",               "TDS return filing for Q4",                              "OVERDUE",   today.minusDays(3)),
                build("Shop Act License Renewal",    "Shop and establishment act license renewal",            "OVERDUE",   today.minusDays(7)),
                build("Professional Tax Filing",     "Professional tax filing for employees",                 "OVERDUE",   today.minusDays(12)),
                build("Import Export Code Renewal",  "IEC renewal with DGFT",                                 "OVERDUE",   today.minusDays(18)),
                build("Trademark Renewal",           "Trademark registration renewal",                        "OVERDUE",   today.minusDays(22)),
                build("Q2 Payroll Compliance",       "Payroll compliance check for Q2",                       "CLOSED",    today.minusDays(45)),
                build("Annual Compliance Report",    "Submission of annual compliance report to board",       "CLOSED",    today.minusDays(90))
        );

        complianceRepository.saveAll(records);
        log.info("Inserted {} compliance records successfully.", records.size());
    }

    private Compliance build(String title, String description, String status, LocalDate dueDate) {
        Compliance c = new Compliance();
        c.setTitle(title);
        c.setDescription(description);
        c.setStatus(status);
        c.setDueDate(dueDate);
        c.setDeleted(false);
        return c;
    }
}
