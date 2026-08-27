package com.automotive.costing.security;

import com.automotive.costing.platform.access.CustomerContext;
import com.automotive.costing.security.ratelimit.RateLimitStore;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Set;

/**
 * Lightweight Idempotency-Key enforcement for critical write paths.
 * Uses RateLimitStore as a short-TTL key store (memory local / Redis prod).
 * Header: Idempotency-Key. Duplicate within window → 409 Conflict.
 */
@Component
public class IdempotencyKeyFilter extends OncePerRequestFilter {

    private static final Set<String> PATH_PREFIXES = Set.of(
            "/api/v1/purchase",
            "/api/v1/approvals",
            "/api/v1/rfq",
            "/api/v1/field-sync"
    );

    private final RateLimitStore store;
    private final int windowSeconds;

    public IdempotencyKeyFilter(RateLimitStore store,
                               @Value("${app.security.idempotency-window-seconds:86400}") int windowSeconds) {
        this.store = store;
        this.windowSeconds = windowSeconds <= 0 ? 86400 : windowSeconds;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!HttpMethod.POST.matches(request.getMethod()) && !HttpMethod.PUT.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        String path = request.getRequestURI();
        boolean protectedPath = PATH_PREFIXES.stream().anyMatch(path::startsWith);
        if (!protectedPath) {
            filterChain.doFilter(request, response);
            return;
        }
        String key = request.getHeader("Idempotency-Key");
        if (key == null || key.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }
        String tenant = CustomerContext.getCustomerId() != null ? CustomerContext.getCustomerId() : "anon";
        String storeKey = "idem:" + tenant + ":" + sha(key + ":" + path + ":" + request.getMethod());
        // tryAcquire false means already seen
        if (!store.tryAcquire(storeKey, 1, windowSeconds)) {
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"IDEMPOTENCY_REPLAY\",\"message\":\"Duplicate Idempotency-Key\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static String sha(String s) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d).substring(0, 32);
        } catch (Exception e) {
            return Integer.toHexString(s.hashCode());
        }
    }
}
