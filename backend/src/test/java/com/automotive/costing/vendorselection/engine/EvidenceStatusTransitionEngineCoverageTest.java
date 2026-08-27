package com.automotive.costing.vendorselection.engine;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.vendorselection.entity.EvidenceStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EvidenceStatusTransitionEngineCoverageTest {

    private final EvidenceStatusTransitionEngine engine = new EvidenceStatusTransitionEngine();

    @Test
    void nullTarget_throws() {
        assertThatThrownBy(() -> engine.assertTransitionAllowed(EvidenceStatus.PENDING, null))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void nullFrom_orSame_noop() {
        assertThatCode(() -> engine.assertTransitionAllowed(null, EvidenceStatus.VERIFIED)).doesNotThrowAnyException();
        assertThatCode(() -> engine.assertTransitionAllowed(EvidenceStatus.PENDING, EvidenceStatus.PENDING)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({
            "PENDING,VERIFIED",
            "PENDING,FAILED",
            "PENDING,WAIVED",
            "PENDING,NOT_REQUIRED",
            "PENDING,EXPIRED",
            "VERIFIED,FAILED",
            "VERIFIED,EXPIRED",
            "VERIFIED,WAIVED",
            "FAILED,VERIFIED",
            "FAILED,PENDING",
            "FAILED,WAIVED",
            "FAILED,EXPIRED",
            "EXPIRED,VERIFIED",
            "EXPIRED,PENDING",
            "EXPIRED,FAILED",
            "EXPIRED,WAIVED",
            "WAIVED,VERIFIED",
            "WAIVED,EXPIRED",
            "WAIVED,PENDING",
            "NOT_REQUIRED,PENDING",
            "NOT_REQUIRED,VERIFIED",
            "NOT_REQUIRED,WAIVED"
    })
    void allowed(EvidenceStatus from, EvidenceStatus to) {
        assertThatCode(() -> engine.assertTransitionAllowed(from, to)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({
            "VERIFIED,PENDING",
            "VERIFIED,NOT_REQUIRED",
            "NOT_REQUIRED,FAILED",
            "NOT_REQUIRED,EXPIRED",
            "WAIVED,FAILED",
            "WAIVED,NOT_REQUIRED"
    })
    void forbidden(EvidenceStatus from, EvidenceStatus to) {
        assertThatThrownBy(() -> engine.assertTransitionAllowed(from, to))
                .isInstanceOf(BusinessRuleViolationException.class);
    }
}
