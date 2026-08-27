package com.automotive.costing.vendorselection.engine;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.vendorselection.entity.SupplierLifecycleStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tester scenarios: qualification lifecycle human gate — APPROVED only from APPROVAL_PENDING.
 */
class LifecycleQaScenariosTest {

    private final SupplierLifecycleTransitionEngine engine = new SupplierLifecycleTransitionEngine();

    @Test
    @DisplayName("QA: Cannot jump DISCOVERED → APPROVED")
    void discoveredToApproved_forbidden() {
        assertThatThrownBy(() -> engine.assertTransitionAllowed(
                SupplierLifecycleStatus.DISCOVERED, SupplierLifecycleStatus.APPROVED))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("QA: Cannot APPROVED from RECOMMENDED (must go APPROVAL_PENDING)")
    void recommendedToApproved_forbidden() {
        assertThatThrownBy(() -> engine.assertTransitionAllowed(
                SupplierLifecycleStatus.RECOMMENDED, SupplierLifecycleStatus.APPROVED))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("QA: APPROVAL_PENDING → APPROVED is allowed (human path)")
    void approvalPendingToApproved_allowed() {
        assertThatCode(() -> engine.assertTransitionAllowed(
                SupplierLifecycleStatus.APPROVAL_PENDING, SupplierLifecycleStatus.APPROVED))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("QA: BLOCKED supplier can only return to UNDER_VERIFICATION")
    void blockedOnlyToUnderVerification() {
        assertThatCode(() -> engine.assertTransitionAllowed(
                SupplierLifecycleStatus.BLOCKED, SupplierLifecycleStatus.UNDER_VERIFICATION))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> engine.assertTransitionAllowed(
                SupplierLifecycleStatus.BLOCKED, SupplierLifecycleStatus.APPROVED))
                .isInstanceOf(BusinessRuleViolationException.class);
    }
}
