package com.automotive.costing.vendorselection.engine;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.vendorselection.entity.SupplierLifecycleStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SupplierLifecycleTransitionEngineTest {

    private final SupplierLifecycleTransitionEngine engine = new SupplierLifecycleTransitionEngine();

    @Test
    void approvedOnlyFromApprovalPending() {
        assertThatCode(() -> engine.assertTransitionAllowed(
                SupplierLifecycleStatus.APPROVAL_PENDING, SupplierLifecycleStatus.APPROVED))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> engine.assertTransitionAllowed(
                SupplierLifecycleStatus.RECOMMENDED, SupplierLifecycleStatus.APPROVED))
                .isInstanceOf(BusinessRuleViolationException.class);
        assertThatThrownBy(() -> engine.assertTransitionAllowed(
                SupplierLifecycleStatus.EVALUATED, SupplierLifecycleStatus.APPROVED))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void discoveredToImported_ok() {
        assertThatCode(() -> engine.assertTransitionAllowed(
                SupplierLifecycleStatus.DISCOVERED, SupplierLifecycleStatus.IMPORTED))
                .doesNotThrowAnyException();
    }
}
