package com.automotive.costing.vendorselection.engine;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.vendorselection.entity.WarrantyClaimStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WarrantyClaimStatusTransitionEngineCoverageTest {

    private final WarrantyClaimStatusTransitionEngine engine = new WarrantyClaimStatusTransitionEngine();

    @Test
    void nullTarget_throws() {
        assertThatThrownBy(() -> engine.assertTransitionAllowed(WarrantyClaimStatus.OPEN, null))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void nullFrom_allowsAnyTarget() {
        assertThatCode(() -> engine.assertTransitionAllowed(null, WarrantyClaimStatus.OPEN))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(WarrantyClaimStatus.class)
    void sameStatus_throws(WarrantyClaimStatus s) {
        assertThatThrownBy(() -> engine.assertTransitionAllowed(s, s))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @ParameterizedTest
    @CsvSource({
            "OPEN,ACCEPTED",
            "OPEN,REJECTED",
            "OPEN,WITHDRAWN",
            "OPEN,CLOSED",
            "ACCEPTED,CLOSED",
            "REJECTED,CLOSED",
            "REJECTED,OPEN",
            "WITHDRAWN,CLOSED"
    })
    void allowedEdges(WarrantyClaimStatus from, WarrantyClaimStatus to) {
        assertThatCode(() -> engine.assertTransitionAllowed(from, to)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({
            "ACCEPTED,OPEN",
            "ACCEPTED,REJECTED",
            "CLOSED,OPEN",
            "CLOSED,ACCEPTED",
            "WITHDRAWN,OPEN",
            "WITHDRAWN,ACCEPTED"
    })
    void forbiddenEdges(WarrantyClaimStatus from, WarrantyClaimStatus to) {
        assertThatThrownBy(() -> engine.assertTransitionAllowed(from, to))
                .isInstanceOf(BusinessRuleViolationException.class);
    }
}
