package com.example.student_management_system.authentication.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that intercepts incoming HTTP requests to validate JWT tokens
 * and set the authentication context for the current user.
 * Extends OncePerRequestFilter to guarantee a single execution per request.
 */

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // Service responsible for parsing and validating JWT tokens
    private final JwtService jwtService;
    // Service used to load user details from the database by username (email)
    private final CustomUserDetailsService userDetailsService;

    /**
     * Constructor for JwtAuthenticationFilter.
     * @param jwtService            // Service for JWT operations
     * @param userDetailsService    // Service for loading user details
     */
    public JwtAuthenticationFilter(JwtService jwtService,
                                   CustomUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

     /**
     * Intercepts each HTTP request to check for a valid JWT token in the
     * Authorization header. If a valid token is found, the authentication
     * context is set for the user.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Read the Authorization header from the incoming request
        String header = request.getHeader("Authorization");

        // If the Authorization header is missing or does not start with "Bearer "
        // continue the filter chain without authentication
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract the JWT token from the Authorization header and validate it. If valid, set the authentication context for the user.
        String token = header.substring(7);

        try {
            // Extract the username (email) from the token's claims
            String email = jwtService.extractUsername(token);

            // Proceed only if an email was extracted and the user is not already authenticated
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails user = userDetailsService.loadUserByUsername(email);

                // Validate the token against the loaded user details (e.g. username match, expiration)
                if (jwtService.isTokenValid(token, user)) {
                    var authentication = new UsernamePasswordAuthenticationToken(
                            user, null, user.getAuthorities());
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
            // If the token is invalid or the user is not found, continue the filter chain without authentication.
        }

        filterChain.doFilter(request, response);
    }
}