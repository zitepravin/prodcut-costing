package com.automotive.costing.platform.access;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Enforces support-access READ_ONLY: blocks mutating HTTP methods while a support session
 * is active on the request. Platform support-access control APIs under /api/v1/platform/support-access
 * remain allowed so sessions can be revoked.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 40)
public class SupportReadOnlyFilter extends OncePerRequestFilter {

    /** Response header so the FE can show the support-session banner (V224). */
    public static final String SUPPORT_RO_HEADER = "X-Support-Read-Only";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (CustomerContext.isSupportReadOnly()) {
            response.setHeader(SUPPORT_RO_HEADER, "true");
            if (isMutating(request.getMethod())) {
                String path = request.getRequestURI() == null ? "" : request.getRequestURI();
                if (!path.contains("/api/v1/platform/support-access")) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter().write(
                            "{\"message\":\"Support access is READ_ONLY — write operations are not permitted\","
                                    + "\"ruleId\":\"SUPPORT-03\"}");
                    return;
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    private static boolean isMutating(String method) {
        if (method == null) return false;
        return HttpMethod.POST.matches(method)
                || HttpMethod.PUT.matches(method)
                || HttpMethod.PATCH.matches(method)
                || HttpMethod.DELETE.matches(method);
    }
}
