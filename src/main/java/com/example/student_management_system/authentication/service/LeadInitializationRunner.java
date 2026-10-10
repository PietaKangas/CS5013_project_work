package com.example.student_management_system.authentication.service;

import com.example.student_management_system.authentication.model.Role;
import com.example.student_management_system.authentication.model.User;
import com.example.student_management_system.authentication.repository.UserRepository;
import com.example.student_management_system.authentication.security.PasswordPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
@Component
public class LeadInitializationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LeadInitializationRunner.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    public LeadInitializationRunner(UserRepository userRepository, PasswordEncoder passwordEncoder, Environment environment) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean isTestProfile = Arrays.asList(environment.getActiveProfiles()).contains("test");
        boolean isDevProfile = Arrays.asList(environment.getActiveProfiles()).contains("dev");

        if (userRepository.countByRole(Role.LEAD) > 0) {
            return;
        }

        String leadEmail = environment.getProperty("LEAD_EMAIL");
        String leadPassword = environment.getProperty("LEAD_PASSWORD");

        if (leadEmail == null || leadEmail.isBlank() || leadPassword == null || leadPassword.isBlank()) {
            if (isTestProfile || isDevProfile) {
                log.warn("Lead bootstrap skipped because LEAD_EMAIL or LEAD_PASSWORD is missing in the {} profile. No lead was created.", Arrays.toString(environment.getActiveProfiles()));
                return;
            }
            throw new IllegalStateException("LEAD_EMAIL and LEAD_PASSWORD must be configured in production before startup.");
        }

        PasswordPolicy.validate(leadPassword, "Lead password");

        String normalizedEmail = leadEmail.trim();
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            log.warn("Startup lead initialization skipped because a conflicting user already exists.");
            return;
        }

        try {
            User leadUser = new User(normalizedEmail, passwordEncoder.encode(leadPassword), Role.LEAD);
            userRepository.save(leadUser);
        } catch (DataIntegrityViolationException e) {
            log.warn("Startup lead initialization skipped because another instance created the lead concurrently.");
        }
    }
}
