package com.example.student_management_system.authentication.util;

import java.util.Locale;

// Utility class for normalizing email addresses by trimming whitespace and converting to lowercase.
public final class EmailNormalizer {
    private EmailNormalizer() {}

    public static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}