package com.automotive.costing.platform.access.tenant;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.platform.access.CustomerContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract tests for data-plane isolation rules.
 * Full integration requires Testcontainers + two seeded customers; this suite locks the
 * TenantAccessGuard / CustomerContext contract used by all hardened services.
 */
class DataPlaneTenantIsolationContractTest {

    private final TenantAccessGuard guard = new TenantAccessGuard();

    @AfterEach
    void clear() {
        CustomerContext.clear();
    }

    @Test
    void requireCustomerId_absent_throws() {
        assertThrows(BusinessRuleViolationException.class, guard::requireCustomerId);
    }

    @Test
    void requireCustomerId_present_returns() {
        CustomerContext.setCustomerId("cust-a");
        assertEquals("cust-a", guard.requireCustomerId());
    }

    @Test
    void assertOwns_sameTenant_ok() {
        CustomerContext.setCustomerId("cust-a");
        assertDoesNotThrow(() -> guard.assertOwns("cust-a", "VehicleProgram", "p1"));
    }

    @Test
    void assertOwns_crossTenant_throws() {
        CustomerContext.setCustomerId("cust-a");
        assertThrows(BusinessRuleViolationException.class,
                () -> guard.assertOwns("cust-b", "VehicleProgram", "p1"));
    }

    @Test
    void assertOwns_nullOwner_throws() {
        CustomerContext.setCustomerId("cust-a");
        assertThrows(BusinessRuleViolationException.class,
                () -> guard.assertOwns(null, "BomNode", "n1"));
    }

    @Test
    void context_doesNotLeakAcrossClear() {
        CustomerContext.setCustomerId("cust-a");
        CustomerContext.clear();
        assertThrows(BusinessRuleViolationException.class, guard::requireCustomerId);
    }
}

// Additional documentation-only markers for V272:
// - ERP batches are tenant-owned (customer_id on erp_inbound_batch)
// - New customers receive catalog seed from DEFAULT (overhead + questions) without transactional data
