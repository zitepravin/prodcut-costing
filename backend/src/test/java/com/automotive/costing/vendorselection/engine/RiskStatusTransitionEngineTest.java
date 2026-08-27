package com.automotive.costing.vendorselection.engine;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.vendorselection.entity.RiskStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RiskStatusTransitionEngineTest {

    private final RiskStatusTransitionEngine engine = new RiskStatusTransitionEngine();

    @Test
    void openToMitigatingAllowed() {
        assertThatCode(() -> engine.assertTransitionAllowed(RiskStatus.OPEN, RiskStatus.MITIGATING))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptedToOpenForbidden() {
        assertThatThrownBy(() -> engine.assertTransitionAllowed(RiskStatus.ACCEPTED, RiskStatus.OPEN))
                .isInstanceOf(BusinessRuleViolationException.class);
    }
}
