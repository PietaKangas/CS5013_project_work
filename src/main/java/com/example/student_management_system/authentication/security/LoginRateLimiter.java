package com.example.student_management_system.authentication.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginRateLimiter {

    private final int maxAttempts;
    private final Duration window;
    private final Map<String, AttemptState> attempts = new ConcurrentHashMap<>();

    @Autowired
    public LoginRateLimiter(@Value("${app.login.max-attempts:5}") Integer maxAttempts,
                           @Value("${app.login.window:PT10M}") Duration window) {
        this.maxAttempts = maxAttempts == null ? 5 : maxAttempts;
        this.window = window == null ? Duration.ofMinutes(10) : window;
    }

    public LoginRateLimiter(int maxAttempts, Duration window) {
        this.maxAttempts = maxAttempts;
        this.window = window == null ? Duration.ofMinutes(10) : window;
    }

    public void checkAllowed(String clientIp, String email) {
        String normalizedIp = normalizeKey(clientIp);
        String normalizedEmail = normalizeKey(email);

        for (String key : Arrays.asList("ip:" + normalizedIp, "email:" + normalizedEmail)) {
            AttemptState state = attempts.get(key);
            if (state == null) {
                continue;
            }

            long now = System.currentTimeMillis();
            if (state.expiresAtEpochMs() <= now) {
                attempts.remove(key);
                continue;
            }

            if (state.count() >= maxAttempts) {
                throw new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Too many login attempts. Please try again later."
                );
            }
        }
    }

    public void recordFailure(String clientIp, String email) {
        incrementAttempt("ip:" + normalizeKey(clientIp));
        incrementAttempt("email:" + normalizeKey(email));
    }

    public void recordSuccess(String clientIp, String email) {
        attempts.remove("ip:" + normalizeKey(clientIp));
        attempts.remove("email:" + normalizeKey(email));
    }

    public void reset() {
        attempts.clear();
    }

    private void incrementAttempt(String key) {
        long now = System.currentTimeMillis();
        AttemptState existing = attempts.get(key);

        if (existing == null || existing.expiresAtEpochMs() <= now) {
            attempts.put(key, new AttemptState(now + window.toMillis(), 1));
            return;
        }

        attempts.put(key, new AttemptState(existing.expiresAtEpochMs(), existing.count() + 1));
    }

    private String normalizeKey(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private record AttemptState(long expiresAtEpochMs, int count) {
    }
}
