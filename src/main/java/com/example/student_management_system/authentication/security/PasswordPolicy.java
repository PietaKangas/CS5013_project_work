package com.example.student_management_system.authentication.security;

import java.nio.charset.StandardCharsets;

import java.util.Locale;
import java.util.Set;

public final class PasswordPolicy {
/*
    private static final int MIN_LENGTH = 12;
    private static final int MAX_LENGTH_BYTES = 72;
    private static final Set<String> COMMON_PASSWORDS = Set.of(
            "password", "password1", "password12", "password123", "password1234", "admin",
            "admin123", "welcome", "welcome123", "letmein", "qwerty", "qwerty123", "abc123",
            "123456", "12345678", "123456789", "dragon", "monkey", "football", "sunshine",
            "princess", "login", "master", "hello", "iloveyou", "secret", "passw0rd"
    );

    private PasswordPolicy() {
    }

    public static void validate(String password, String fieldName) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }

        int byteLength = password.getBytes(StandardCharsets.UTF_8).length;
        if (byteLength < MIN_LENGTH) {
            throw new IllegalArgumentException("Password must be at least 12 characters long.");
        }
        if (byteLength > MAX_LENGTH_BYTES) {
            throw new IllegalArgumentException("Password must be at most 72 bytes long.");
        }

        String normalized = password.toLowerCase(Locale.ROOT);
        if (COMMON_PASSWORDS.contains(normalized)) {
            throw new IllegalArgumentException("Password is too common; choose a different password.");
        }
    }
}

*/
    private static final int MIN_LENGTH = 12;
    private static final int MAX_LENGTH_BYTES = 72;

    private PasswordPolicy() {
    }

    public static void validate(String password, String fieldName) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }

        int length = password.codePointCount(0, password.length());
        if (length < MIN_LENGTH) {
            throw new IllegalArgumentException(fieldName + " must be at least " + MIN_LENGTH + " characters long.");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_LENGTH_BYTES) {
            throw new IllegalArgumentException(fieldName + " must be at most " + MAX_LENGTH_BYTES + " bytes long.");
        }
    }
}
