package com.example.student_management_system.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Body of POST /api/auth/login. */
public record LoginRequest(
        @NotBlank(message = "Email is required")
        String email,

        @NotBlank(message = "Password is required")
        String password
) {}
