package com.example.student_management_system.authentication.service;

import com.example.student_management_system.authentication.dto.AuthResponse;
import com.example.student_management_system.authentication.dto.LoginRequest;
import com.example.student_management_system.authentication.dto.SignupRequest;
import com.example.student_management_system.authentication.model.Role;
import com.example.student_management_system.authentication.model.User;
import com.example.student_management_system.authentication.repository.UserRepository;
import com.example.student_management_system.authentication.security.JwtService;
import com.example.student_management_system.authentication.security.PasswordPolicy;
import com.example.student_management_system.authentication.util.EmailNormalizer;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

// Service class for handling user authentication, including signup and login operations.
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

    // Handles user signup by validating the request, checking for existing users, encoding the password, and saving the new user.
    public AuthResponse signup(SignupRequest request) {
        String email = EmailNormalizer.normalize(request.getEmail());
        String password = request.getPassword();

        if (email.isEmpty() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Email and password are required");
        }

        PasswordPolicy.validate(password, "Password");

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        String encodedPassword = passwordEncoder.encode(password);
        User user = new User(email, encodedPassword, Role.STUDENT);
        User savedUser = userRepository.save(user);

        return buildResponse(savedUser);
    }

    // Handles user login by authenticating the provided credentials and generating a JWT token upon successful authentication.
    public AuthResponse login(LoginRequest request) {
        String email = EmailNormalizer.normalize(request.getEmail());
        String password = request.getPassword();

        if (email.isEmpty() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Email and password are required");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        return buildResponse(user, userDetails);
    }

    // Builds an AuthResponse containing the JWT token, email, and role of the authenticated user.
    private AuthResponse buildResponse(User user) {
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .roles(user.getRole().name())
                .build();

        return buildResponse(user, userDetails);
    }

    private AuthResponse buildResponse(User user, UserDetails userDetails) {
        String token = jwtService.generateToken(userDetails);
        return new AuthResponse(token, user.getEmail(), user.getRole().name());
    }

}
