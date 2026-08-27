package com.automotive.costing.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Production observability — ensures every request carries a correlation/request ID
 * that is written to MDC (for structured logs), returned as a response header, and
 * available to downstream services, queue workers, and email delivery.
 *
 * Accepts client-supplied {@code X-Request-Id} / {@code X-Correlation-Id} when present
 * and well-formed; otherwise generates a stable, human-readable identifier.
 *
 * Format example: REQ-20260818-8F72A3C1
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER_REQUEST_ID = "X-Request-Id";
    public static final String HEADER_CORRELATION_ID = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;
    private static final int MAX_INBOUND_LEN = 64;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String correlationId = resolveCorrelationId(request);
        MDC.put(MDC_KEY, correlationId);
        try {
            response.setHeader(HEADER_REQUEST_ID, correlationId);
            response.setHeader(HEADER_CORRELATION_ID, correlationId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        String inbound = firstNonBlank(
                request.getHeader(HEADER_REQUEST_ID),
                request.getHeader(HEADER_CORRELATION_ID));
        if (inbound != null && isSafeToken(inbound)) {
            return inbound.trim();
        }
        return generate();
    }

    private static String generate() {
        String day = LocalDate.now().format(DAY);
        String suffix = Long.toHexString(ThreadLocalRandom.current().nextLong() & 0xFFFFFFFFL).toUpperCase();
        // pad to 8 hex chars
        suffix = ("00000000" + suffix).substring(suffix.length());
        return "REQ-" + day + "-" + suffix;
    }

    private static boolean isSafeToken(String value) {
        if (value == null) {
            return false;
        }
        String t = value.trim();
        if (t.isEmpty() || t.length() > MAX_INBOUND_LEN) {
            return false;
        }
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (!(Character.isLetterOrDigit(c) || c == '-' || c == '_' || c == '.')) {
                return false;
            }
        }
        return true;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        return null;
    }

    /**
     * Utility for services / workers that need the current correlation id.
     */
    public static String current() {
        String v = MDC.get(MDC_KEY);
        if (v != null && !v.isBlank()) {
            return v;
        }
        return "REQ-UNKNOWN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
