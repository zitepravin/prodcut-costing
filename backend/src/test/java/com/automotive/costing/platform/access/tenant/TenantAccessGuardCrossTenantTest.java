package com.automotive.costing.platform.access.tenant;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.platform.access.CustomerContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure unit tests (no Docker, no DB) for cross-tenant denial.
 */
class TenantAccessGuardCrossTenantTest {

    private final TenantAccessGuard guard = new TenantAccessGuard();

    @BeforeEach
    void setUp() {
        CustomerContext.clear();
    }

    @AfterEach
    void tearDown() {
        CustomerContext.clear();
    }

    @Test
    void sameTenant_allows() {
        CustomerContext.setCustomerId("cust-A");
        assertThatCode(() -> guard.assertOwns("cust-A", "VehicleProgram", "p1"))
                .doesNotThrowAnyException();
    }

    @Test
    void otherTenant_denies() {
        CustomerContext.setCustomerId("cust-A");
        assertThatThrownBy(() -> guard.assertOwns("cust-B", "VehicleProgram", "p1"))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void missingContext_deniesRequire() {
        CustomerContext.clear();
        assertThatThrownBy(guard::requireCustomerId)
                .isInstanceOfAny(BusinessRuleViolationException.class, IllegalStateException.class);
    }
}
