package com.example.student_management_system.authentication.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    @Test
    void rejectsShortSecret() {
        assertThatThrownBy(() -> new JwtService("short-secret", 3600000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 32 bytes");
    }
}
