package com.automotive.costing.vendorselection.engine;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.vendorselection.entity.SupplierLifecycleStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SupplierLifecycleTransitionEngineCoverageTest {

    private final SupplierLifecycleTransitionEngine engine = new SupplierLifecycleTransitionEngine();

    @Test
    void nullTarget_throws() {
        assertThatThrownBy(() -> engine.assertTransitionAllowed(SupplierLifecycleStatus.DISCOVERED, null))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void nullFrom_allows() {
        assertThatCode(() -> engine.assertTransitionAllowed(null, SupplierLifecycleStatus.DISCOVERED))
                .doesNotThrowAnyException();
    }

    @Test
    void same_throws() {
        assertThatThrownBy(() -> engine.assertTransitionAllowed(
                SupplierLifecycleStatus.APPROVED, SupplierLifecycleStatus.APPROVED))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @ParameterizedTest
    @CsvSource({
            "DISCOVERED,IMPORTED",
            "DISCOVERED,UNDER_VERIFICATION",
            "IMPORTED,UNDER_VERIFICATION",
            "UNDER_VERIFICATION,QUALIFIED",
            "QUALIFIED,EVALUATED",
            "EVALUATED,RECOMMENDED",
            "RECOMMENDED,APPROVAL_PENDING",
            "APPROVAL_PENDING,APPROVED",
            "APPROVAL_PENDING,REJECTED",
            "APPROVED,SUSPENDED",
            "BLOCKED,UNDER_VERIFICATION",
            "SUSPENDED,QUALIFIED",
            "REJECTED,UNDER_VERIFICATION"
    })
    void allowed(SupplierLifecycleStatus from, SupplierLifecycleStatus to) {
        assertThatCode(() -> engine.assertTransitionAllowed(from, to)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({
            "DISCOVERED,APPROVED",
            "APPROVED,APPROVAL_PENDING",
            "BLOCKED,APPROVED",
            "RECOMMENDED,APPROVED"
    })
    void forbidden(SupplierLifecycleStatus from, SupplierLifecycleStatus to) {
        assertThatThrownBy(() -> engine.assertTransitionAllowed(from, to))
                .isInstanceOf(BusinessRuleViolationException.class);
    }
}
