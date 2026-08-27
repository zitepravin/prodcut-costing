package com.automotive.costing.platform.access.tenant;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.platform.access.CustomerContext;
import org.springframework.stereotype.Component;

/**
 * Central enforcement for data-plane multi-tenancy.
 *
 * <p>Rules:
 * <ul>
 *   <li>Current customer comes only from {@link CustomerContext} (set by filter after membership check).</li>
 *   <li>Never trust a customer_id supplied solely in the request body for authorization.</li>
 *   <li>Missing tenant context on a tenant-owned operation is a hard failure (ACCESS-TENANT-01).</li>
 *   <li>Entity owned by another customer is treated as not visible (ACCESS-TENANT-02) — do not leak existence.</li>
 * </ul>
 */
@Component
public class TenantAccessGuard {

    public static final String RULE_MISSING_CONTEXT = "ACCESS-TENANT-01";
    public static final String RULE_CROSS_TENANT = "ACCESS-TENANT-02";

    /**
     * @return non-blank customer id from context
     * @throws BusinessRuleViolationException if no tenant context is active
     */
    public String requireCustomerId() {
        String cid = CustomerContext.getCustomerId();
        if (cid == null || cid.isBlank()) {
            throw new BusinessRuleViolationException(
                    "Tenant context required (X-Customer-Id with active membership)",
                    RULE_MISSING_CONTEXT);
        }
        return cid.trim();
    }

    /**
     * Assert that a loaded entity's customer_id matches the current tenant.
     * Call after loading by primary key so cross-tenant IDs cannot be read.
     */
    public void assertOwns(String entityCustomerId, String entityLabel, String entityId) {
        String current = requireCustomerId();
        if (entityCustomerId == null || entityCustomerId.isBlank() || !current.equals(entityCustomerId)) {
            // Same message shape as not-found to avoid IDOR oracle; rule id distinguishes in logs
            throw new BusinessRuleViolationException(
                    entityLabel + " not found or not accessible in current tenant: " + entityId,
                    RULE_CROSS_TENANT);
        }
    }

    /**
     * Optional: when support read-only session, block mutating operations.
     */
    public void assertNotSupportReadOnly() {
        if (CustomerContext.isSupportReadOnly()) {
            throw new BusinessRuleViolationException(
                    "Support access is read-only for this customer",
                    "ACCESS-SUPPORT-RO");
        }
    }
}
