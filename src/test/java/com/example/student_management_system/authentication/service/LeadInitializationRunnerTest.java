package com.example.student_management_system.authentication.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.example.student_management_system.authentication.model.Role;
import com.example.student_management_system.authentication.model.User;
import com.example.student_management_system.authentication.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// check tests
@ExtendWith(MockitoExtension.class)
class LeadInitializationRunnerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Environment environment;

    private LeadInitializationRunner runner;

    @BeforeEach
    void setUp() {
        runner = new LeadInitializationRunner(userRepository, passwordEncoder, environment);
    }

    @Test
    void createsLeadWhenNoneExists() {
        when(environment.getActiveProfiles()).thenReturn(new String[] {"prod"});
        when(environment.getProperty("LEAD_EMAIL")).thenReturn("lead@example.com");
        when(environment.getProperty("LEAD_PASSWORD")).thenReturn("StrongPassword123!");
        when(userRepository.countByRole(Role.LEAD)).thenReturn(0L);
        when(userRepository.findByEmail("lead@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("StrongPassword123!")).thenReturn("hashed-lead-password");

        runner.run(new DefaultApplicationArguments(new String[0]));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("lead@example.com");
        assertThat(saved.getPasswordHash()).isEqualTo("hashed-lead-password");
        assertThat(saved.getRole()).isEqualTo(Role.LEAD);
    }

    @Test
    void doesNotResetExistingLeadPasswordOnSecondRun() {
        when(environment.getActiveProfiles()).thenReturn(new String[] {"prod"});
        when(userRepository.countByRole(Role.LEAD)).thenReturn(1L);

        runner.run(new DefaultApplicationArguments(new String[0]));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void throwsClearExceptionInProdWhenLeadConfigMissing() {
        when(environment.getActiveProfiles()).thenReturn(new String[] {"prod"});
        when(environment.getProperty("LEAD_EMAIL")).thenReturn(" ");
        when(environment.getProperty("LEAD_PASSWORD")).thenReturn("");
        when(userRepository.countByRole(Role.LEAD)).thenReturn(0L);

        assertThatThrownBy(() -> runner.run(new DefaultApplicationArguments(new String[0])))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("LEAD_EMAIL")
                .hasMessageContaining("LEAD_PASSWORD");
    }

    @Test
    void logsWarningAndDoesNotFailInDevWhenLeadConfigMissing() {
        when(environment.getActiveProfiles()).thenReturn(new String[] {"dev"});
        when(environment.getProperty("LEAD_EMAIL")).thenReturn(null);
        when(environment.getProperty("LEAD_PASSWORD")).thenReturn(null);
        when(userRepository.countByRole(Role.LEAD)).thenReturn(0L);

        Logger logger = (Logger) LoggerFactory.getLogger(LeadInitializationRunner.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        try {
            runner.run(new DefaultApplicationArguments(new String[0]));
            assertThat(listAppender.list)
                    .anySatisfy(event -> assertThat(event.getFormattedMessage()).contains("Lead bootstrap skipped"));
            verify(userRepository, never()).save(any(User.class));
        } finally {
            logger.detachAppender(listAppender);
        }
    }

    @Test
    void rejectsPasswordThatViolatesPasswordPolicy() {
        when(environment.getActiveProfiles()).thenReturn(new String[] {"prod"});
        when(environment.getProperty("LEAD_EMAIL")).thenReturn("lead@example.com");
        when(environment.getProperty("LEAD_PASSWORD")).thenReturn("short");
        when(userRepository.countByRole(Role.LEAD)).thenReturn(0L);

        assertThatThrownBy(() -> runner.run(new DefaultApplicationArguments(new String[0])))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 12 characters");
    }
}
