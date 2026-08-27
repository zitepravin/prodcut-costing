package com.automotive.costing.platform.access.tenant;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.platform.access.CustomerContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TenantAccessGuardTest {

    private final TenantAccessGuard guard = new TenantAccessGuard();

    @AfterEach
    void clear() {
        CustomerContext.clear();
    }

    @Test
    void requireCustomerId_missing_throws() {
        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
                guard::requireCustomerId);
        assertEquals(TenantAccessGuard.RULE_MISSING_CONTEXT, ex.getRuleReference());
    }

    @Test
    void requireCustomerId_present_returns() {
        CustomerContext.setCustomerId("cust-a");
        assertEquals("cust-a", guard.requireCustomerId());
    }

    @Test
    void assertOwns_crossTenant_throws() {
        CustomerContext.setCustomerId("cust-a");
        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
                () -> guard.assertOwns("cust-b", "VehicleProgram", "prog-1"));
        assertEquals(TenantAccessGuard.RULE_CROSS_TENANT, ex.getRuleReference());
    }

    @Test
    void assertOwns_sameTenant_ok() {
        CustomerContext.setCustomerId("cust-a");
        assertDoesNotThrow(() -> guard.assertOwns("cust-a", "VehicleProgram", "prog-1"));
    }

    @Test
    void assertNotSupportReadOnly_blocksWhenSupport() {
        CustomerContext.setCustomerId("cust-a");
        CustomerContext.setSupportReadOnly(true);
        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
                guard::assertNotSupportReadOnly);
        assertEquals("ACCESS-SUPPORT-RO", ex.getRuleReference());
    }
}
