package com.example.student_management_system.auth;

import com.example.student_management_system.auth.dto.AuthResponse;
import com.example.student_management_system.auth.dto.LoginRequest;
import com.example.student_management_system.auth.dto.SignupRequest;
import com.example.student_management_system.auth.dto.UserResponse;
import com.example.student_management_system.common.ApiException;
import com.example.student_management_system.security.AuthUser;
import com.example.student_management_system.security.JwtService;
import com.example.student_management_system.user.AppUser;
import com.example.student_management_system.user.Role;
import com.example.student_management_system.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/** Business logic for creating accounts and logging in. */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    /** Public signup: always creates a STUDENT, whatever the client sends. */
    @Transactional
    public UserResponse registerStudent(SignupRequest request) {
        return createAccount(request, Role.STUDENT);
    }

    /** Only reachable by an existing Lead (see LeadAccountController). */
    @Transactional
    public UserResponse createLead(SignupRequest request) {
        return createAccount(request, Role.LEAD);
    }

    public AuthResponse login(LoginRequest request) {
        // Spring Security loads the user and checks the BCrypt hash.
        // Wrong email or password -> BadCredentialsException -> 401 (GlobalExceptionHandler).
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizeEmail(request.email()), request.password()));

        AuthUser principal = (AuthUser) auth.getPrincipal();
        AppUser user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        String token = jwtService.generateToken(principal);
        return AuthResponse.bearer(token, jwtService.getExpirationSeconds(), UserResponse.from(user));
    }

    public UserResponse currentUser(AuthUser me) {
        return userRepository.findById(me.getId())
                .map(UserResponse::from)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Account no longer exists"));
    }

    private UserResponse createAccount(SignupRequest request, Role role) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        AppUser user = new AppUser(
                request.fullName().trim(),
                email,
                passwordEncoder.encode(request.password()),
                role);
        try {
            return UserResponse.from(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException e) {
            // two signups with the same email at the same moment: the UNIQUE constraint catches it
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
    }

    static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
