package com.automotive.costing.helpingagent.service;

import com.automotive.costing.platform.access.CustomerContext;
import com.automotive.costing.platform.access.service.CentralAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HelpingAgentAuthorizationService {
    public static final String MODULE_PRODUCT_COSTING = CentralAccessService.MODULE_PRODUCT_COSTING;
    private final CentralAccessService centralAccessService;

    public void assertCanUseAgent() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null
                || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AccessDeniedException("Authentication required for Helping Agent");
        }
        String customerId = CustomerContext.getCustomerId();
        if (customerId == null || customerId.isBlank()) {
            return;
        }
        centralAccessService.assertModuleEnabled(customerId, MODULE_PRODUCT_COSTING);
    }

    public String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "unknown";
    }

    public String currentCustomerId() {
        return CustomerContext.getCustomerId();
    }
}
