package com.example.student_management_system.common;

import java.time.Instant;
import java.util.Map;

/**
 * The JSON body of every error response, e.g.
 * {"status":400,"error":"Bad Request","message":"Validation failed","fieldErrors":{"email":"must be a well-formed email address"},...}
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors
) {
    public static ApiError of(int status, String error, String message) {
        return new ApiError(Instant.now(), status, error, message, Map.of());
    }
}
