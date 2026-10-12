package com.example.student_management_system.config;

import com.example.student_management_system.security.CustomUserDetailsService;
import com.example.student_management_system.security.JwtAuthenticationFilter;
import com.example.student_management_system.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Central security rules:
 *  - no server sessions; every request must carry a JWT
 *  - /api/auth/signup and /api/auth/login are public, everything else under /api needs a login
 *  - /api/admin/** is for International Leads only
 */
@Configuration
@EnableMethodSecurity   // lets later modules use @PreAuthorize("hasRole('LEAD')") on methods
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtService jwtService,
                                                   CustomUserDetailsService userDetailsService) throws Exception {
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtService, userDetailsService);

        http
            .csrf(AbstractHttpConfigurer::disable)          // safe: we use bearer tokens, not cookies
            .cors(Customizer.withDefaults())                // uses the corsConfigurationSource bean below
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()                      // CORS pre-flight
                .requestMatchers(HttpMethod.POST, "/api/auth/signup", "/api/auth/login").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/api/admin/**").hasRole("LEAD")
                .anyRequest().authenticated()
            )
            .exceptionHandling(e -> e
                // not logged in / bad token -> 401
                .authenticationEntryPoint((req, res, ex) ->
                    writeJsonError(res, 401, "Unauthorized", "Please log in to access this resource"))
                // logged in but wrong role -> 403
                .accessDeniedHandler((req, res, ex) ->
                    writeJsonError(res, 403, "Forbidden", "You do not have permission to do this"))
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Built by Spring from our UserDetailsService + PasswordEncoder; AuthService uses it to check logins.
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // Allows the React dev server (another port = another "origin") to call the API.
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") String allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setExposedHeaders(List.of("Content-Disposition"));   // needed later for file downloads
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    private static void writeJsonError(HttpServletResponse res, int status, String error, String message)
            throws IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.getWriter().write(
            "{\"status\":" + status + ",\"error\":\"" + error + "\",\"message\":\"" + message + "\",\"fieldErrors\":{}}");
    }
}
