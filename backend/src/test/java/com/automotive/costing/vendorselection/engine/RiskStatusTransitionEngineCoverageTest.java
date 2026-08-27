package com.automotive.costing.vendorselection.engine;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.vendorselection.entity.RiskStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RiskStatusTransitionEngineCoverageTest {

    private final RiskStatusTransitionEngine engine = new RiskStatusTransitionEngine();

    @Test
    void nullTarget_throws() {
        assertThatThrownBy(() -> engine.assertTransitionAllowed(RiskStatus.OPEN, null))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void nullFrom_orSame_noop() {
        assertThatCode(() -> engine.assertTransitionAllowed(null, RiskStatus.CLOSED)).doesNotThrowAnyException();
        assertThatCode(() -> engine.assertTransitionAllowed(RiskStatus.OPEN, RiskStatus.OPEN)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({
            "OPEN,MITIGATING",
            "OPEN,ACCEPTED",
            "OPEN,CLOSED",
            "MITIGATING,OPEN",
            "MITIGATING,ACCEPTED",
            "MITIGATING,CLOSED",
            "ACCEPTED,CLOSED",
            "ACCEPTED,MITIGATING",
            "CLOSED,OPEN",
            "CLOSED,MITIGATING"
    })
    void allowed(RiskStatus from, RiskStatus to) {
        assertThatCode(() -> engine.assertTransitionAllowed(from, to)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({
            "ACCEPTED,OPEN",
            "CLOSED,ACCEPTED"
    })
    void forbidden(RiskStatus from, RiskStatus to) {
        assertThatThrownBy(() -> engine.assertTransitionAllowed(from, to))
                .isInstanceOf(BusinessRuleViolationException.class);
    }
}
