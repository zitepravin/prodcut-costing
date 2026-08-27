package com.automotive.costing.security;

import com.automotive.costing.platform.access.CustomerContext;
import com.automotive.costing.security.ratelimit.RateLimitStore;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Tenant- and user-aware API rate limiting.
 * Store is pluggable: memory (default) or Redis (app.security.rate-limit-store=redis).
 */
@Component
public class ApiRateLimitFilter extends OncePerRequestFilter {

    private final RateLimitStore rateLimitStore;

    @Value("${app.security.api-rate-limit-enabled:true}")
    private boolean enabled;

    @Value("${app.security.api-rate-limit-max:600}")
    private int maxRequests;

    @Value("${app.security.api-rate-limit-window-seconds:60}")
    private int windowSeconds;

    @Value("${app.security.upload-rate-limit-max:30}")
    private int uploadMax;

    public ApiRateLimitFilter(RateLimitStore rateLimitStore) {
        this.rateLimitStore = rateLimitStore;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null) {
            return true;
        }
        return path.startsWith("/actuator")
                || path.startsWith("/api/v1/auth/")
                || path.startsWith("/swagger")
                || path.startsWith("/api-docs")
                || path.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String bucket = classify(request);
        int limit = "upload".equals(bucket) ? uploadMax : maxRequests;
        String key = bucketKey(bucket);

        if (!rateLimitStore.tryAcquire(key, limit, windowSeconds)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"message\":\"Rate limit exceeded for bucket '" + bucket
                            + "'. Retry later.\",\"ruleId\":\"RATE-LIMIT-01\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String classify(HttpServletRequest request) {
        String path = request.getRequestURI() == null ? "" : request.getRequestURI();
        String method = request.getMethod() == null ? "GET" : request.getMethod();
        String ct = request.getContentType() == null ? "" : request.getContentType();
        if (path.contains("/bulk-import") || path.contains("/import")
                || path.contains("/upload") || path.contains("/files")
                || ct.toLowerCase().startsWith("multipart/")) {
            return "upload";
        }
        if (path.contains("/report") || path.contains("/export")) {
            return "report";
        }
        if (path.contains("/helping-agent") || path.contains("/ai/")) {
            return "ai";
        }
        if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method)) {
            return "write";
        }
        return "api";
    }

    private String bucketKey(String bucket) {
        String customer = CustomerContext.getCustomerId();
        if (customer == null || customer.isBlank()) {
            customer = "anon";
        }
        String user = "anon";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
            user = auth.getName();
        }
        return bucket + ":" + customer + ":" + user;
    }
}
