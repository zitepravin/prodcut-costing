package com.automotive.costing.vendorselection.engine;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.vendorselection.entity.EvidenceStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EvidenceStatusTransitionEngineTest {

    private final EvidenceStatusTransitionEngine engine = new EvidenceStatusTransitionEngine();

    @Test
    void pendingToVerifiedAllowed() {
        assertThatCode(() -> engine.assertTransitionAllowed(EvidenceStatus.PENDING, EvidenceStatus.VERIFIED))
                .doesNotThrowAnyException();
    }

    @Test
    void verifiedToPendingForbidden() {
        assertThatThrownBy(() -> engine.assertTransitionAllowed(EvidenceStatus.VERIFIED, EvidenceStatus.PENDING))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void expiredToVerifiedAllowed() {
        assertThatCode(() -> engine.assertTransitionAllowed(EvidenceStatus.EXPIRED, EvidenceStatus.VERIFIED))
                .doesNotThrowAnyException();
    }
}
