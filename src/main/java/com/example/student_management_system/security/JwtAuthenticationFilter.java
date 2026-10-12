package com.example.student_management_system.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Runs once per request. If the request has "Authorization: Bearer <token>" and the
 * token is valid, it marks the request as authenticated as that user.
 * If there is no token or it's invalid, the request simply continues unauthenticated
 * and SecurityConfig decides whether that's allowed (protected URLs then get 401).
 *
 * Deliberately NOT a @Component: SecurityConfig creates it, so Spring Boot doesn't also
 * register it as a normal servlet filter (which would make it run in the wrong place).
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(7);
            jwtService.extractValidEmail(token).ifPresent(email -> authenticate(email, request));
        }

        chain.doFilter(request, response);
    }

    private void authenticate(String email, HttpServletRequest request) {
        try {
            // Load from the database so a deleted account's old token stops working.
            AuthUser user = (AuthUser) userDetailsService.loadUserByUsername(email);
            var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (UsernameNotFoundException ignored) {
            // account no longer exists -> stay unauthenticated
        }
    }
}
