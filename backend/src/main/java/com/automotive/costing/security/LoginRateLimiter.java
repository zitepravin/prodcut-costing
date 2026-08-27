package com.automotive.costing.security;

import com.automotive.costing.config.SecurityProperties;
import com.automotive.costing.security.ratelimit.RateLimitStore;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Rate limiter for /auth/login and /auth/refresh (key = username or IP).
 * Uses the same {@link RateLimitStore} as API rate limiting (memory or Redis).
 */
@Component
public class LoginRateLimiter {

    private final SecurityProperties properties;
    private final RateLimitStore rateLimitStore;

    public LoginRateLimiter(SecurityProperties properties, RateLimitStore rateLimitStore) {
        this.properties = properties;
        this.rateLimitStore = rateLimitStore;
    }

    public void check(String key) {
        if (key == null || key.isBlank()) {
            key = "unknown";
        }
        String storeKey = "login:" + key.trim().toLowerCase();
        int max = properties.getLoginRateLimitMaxAttempts();
        int window = properties.getLoginRateLimitWindowSeconds();
        if (!rateLimitStore.tryAcquire(storeKey, max, window)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many authentication attempts. Try again later.");
        }
    }

    /** Optional explicit reset is a no-op for fixed-window stores (window TTL handles expiry). */
    public void reset(String key) {
        // Fixed-window stores expire keys automatically; no-op for compatibility.
    }
}
