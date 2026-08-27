package com.automotive.costing.platform.access;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.platform.access.service.CentralAccessService;
import com.automotive.costing.platform.access.service.SupportAccessService;
import com.automotive.costing.platform.user.entity.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Validates X-Customer-Id against active membership OR time-limited approved support access.
 * Platform operators without membership/support cannot enter a tenant context.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 50)
@RequiredArgsConstructor
public class CustomerContextFilter extends OncePerRequestFilter {

    private final CentralAccessService centralAccessService;
    private final SupportAccessService supportAccessService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String customerId = request.getHeader(CustomerContext.HEADER);
            if (customerId != null && !customerId.isBlank()) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getPrincipal() instanceof User user) {
                    try {
                        String cid = customerId.trim();
                        if (centralAccessService.hasActiveMembership(cid, user.getUserId())) {
                            // membership still requires customer ACTIVE
                            centralAccessService.assertUserInCustomer(cid, user.getUserId());
                            CustomerContext.setCustomerId(cid);
                        } else if (supportAccessService.hasActiveSupportAccess(cid, user.getUserId())) {
                            centralAccessService.assertCustomerActive(cid);
                            CustomerContext.setCustomerId(cid);
                            CustomerContext.setSupportReadOnly(true);
                        } else {
                            throw new BusinessRuleViolationException(
                                    "User is not an active member of this customer and has no support access",
                                    "ACCESS-06");
                        }
                    } catch (BusinessRuleViolationException ex) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json");
                        response.getWriter().write(
                                "{\"message\":\"" + ex.getMessage().replace("\"", "'")
                                        + "\",\"ruleId\":\"" + (ex.getRuleReference() != null ? ex.getRuleReference() : "ACCESS-06")
                                        + "\"}");
                        return;
                    }
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            CustomerContext.clear();
        }
    }
}
