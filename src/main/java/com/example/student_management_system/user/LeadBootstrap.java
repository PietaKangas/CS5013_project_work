package com.example.student_management_system.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Creates the very first International Lead on startup, from the LEAD_EMAIL and
 * LEAD_PASSWORD environment variables. Runs every start but does nothing if that
 * email already exists. After this, that Lead can create more Leads via POST /api/admin/leads.
 */
@Component
@Order(1)   // before DemoDataLoader
public class LeadBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LeadBootstrap.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String fullName;

    public LeadBootstrap(UserRepository userRepository,
                         PasswordEncoder passwordEncoder,
                         @Value("${app.bootstrap-lead.email}") String email,
                         @Value("${app.bootstrap-lead.password}") String password,
                         @Value("${app.bootstrap-lead.full-name}") String fullName) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            log.info("LEAD_EMAIL / LEAD_PASSWORD not set: no bootstrap Lead account created.");
            return;
        }
        if (password.length() < 8 || password.length() > 72) {
            log.warn("LEAD_PASSWORD must be 8-72 characters: bootstrap Lead account NOT created.");
            return;
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(normalized)) {
            log.info("Bootstrap Lead {} already exists.", normalized);
            return;
        }
        userRepository.save(new AppUser(fullName, normalized, passwordEncoder.encode(password), Role.LEAD));
        log.info("Created bootstrap International Lead account {}", normalized);
    }
}
