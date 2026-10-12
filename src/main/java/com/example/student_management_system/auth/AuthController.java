package com.example.student_management_system.auth;

import com.example.student_management_system.auth.dto.AuthResponse;
import com.example.student_management_system.auth.dto.LoginRequest;
import com.example.student_management_system.auth.dto.SignupRequest;
import com.example.student_management_system.auth.dto.UserResponse;
import com.example.student_management_system.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * POST /api/auth/signup  -> create a Student account (public)
 * POST /api/auth/login   -> get a JWT (public)
 * GET  /api/auth/me      -> who am I? (needs a token; the frontend uses it on page reload)
 *
 * Logout is handled by the frontend deleting its stored token (JWTs are stateless).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.registerStudent(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthUser me) {
        return authService.currentUser(me);
    }
}
