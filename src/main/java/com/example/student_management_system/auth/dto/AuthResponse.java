package com.example.student_management_system.auth.dto;

/**
 * Returned after a successful login. The frontend stores "token", sends it as
 * "Authorization: Bearer <token>", and uses user.role to pick the dashboard.
 */
public record AuthResponse(String token, String tokenType, long expiresInSeconds, UserResponse user) {

    public static AuthResponse bearer(String token, long expiresInSeconds, UserResponse user) {
        return new AuthResponse(token, "Bearer", expiresInSeconds, user);
    }
}
