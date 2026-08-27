package com.automotive.costing.vendorselection.engine;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.vendorselection.entity.WarrantyClaimStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WarrantyClaimStatusTransitionEngineTest {

    private final WarrantyClaimStatusTransitionEngine engine = new WarrantyClaimStatusTransitionEngine();

    @Test
    void openToAcceptedAllowed() {
        assertThatCode(() -> engine.assertTransitionAllowed(WarrantyClaimStatus.OPEN, WarrantyClaimStatus.ACCEPTED))
                .doesNotThrowAnyException();
    }

    @Test
    void closedIsTerminal() {
        assertThatThrownBy(() -> engine.assertTransitionAllowed(WarrantyClaimStatus.CLOSED, WarrantyClaimStatus.OPEN))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void acceptedToOpenForbidden() {
        assertThatThrownBy(() -> engine.assertTransitionAllowed(WarrantyClaimStatus.ACCEPTED, WarrantyClaimStatus.OPEN))
                .isInstanceOf(BusinessRuleViolationException.class);
    }
}
