package com.example.student_management_system.authentication.controller;

import com.example.student_management_system.authentication.dto.AuthResponse;
import com.example.student_management_system.authentication.dto.LoginRequest;
import com.example.student_management_system.authentication.dto.SignupRequest;
import com.example.student_management_system.authentication.security.LoginRateLimiter;
import com.example.student_management_system.authentication.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

// API controller for authentication-related endpoints, including signup and login
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final LoginRateLimiter loginRateLimiter;

    public AuthController(AuthService authService, LoginRateLimiter loginRateLimiter) {
        this.authService = authService;
        this.loginRateLimiter = loginRateLimiter;
    }

    // Endpoint for user signup. Accepts a SignupRequest and returns an AuthResponse upon successful registration.
    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@RequestBody SignupRequest request) {
        try {
            AuthResponse response = authService.signup(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    // Endpoint for user login. Accepts a LoginRequest and returns an AuthResponse upon successful authentication.
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request, HttpServletRequest httpServletRequest) {
        String email = request == null ? "" : request.getEmail();
        String clientIp = resolveClientIp(httpServletRequest);

        // Check if the login attempt is allowed based on rate limiting
        try {
            loginRateLimiter.checkAllowed(clientIp, email);
            AuthResponse response = authService.login(request);
            loginRateLimiter.recordSuccess(clientIp, email);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (ResponseStatusException e) {
            if (e.getStatusCode().value() == HttpStatus.TOO_MANY_REQUESTS.value()) {
                throw e;
            }
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        } catch (AuthenticationException e) {
            loginRateLimiter.recordFailure(clientIp, email);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
    }

    // Resolves the client's IP address from the HttpServletRequest, considering possible proxy headers.
    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }
}
