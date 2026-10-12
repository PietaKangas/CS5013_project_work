package com.example.student_management_system.common;

import org.springframework.http.HttpStatus;

/**
 * Throw this from a service when a request can't be completed, e.g.
 * {@code throw new ApiException(HttpStatus.CONFLICT, "Email is already registered");}
 * GlobalExceptionHandler turns it into a JSON error response with that status.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
