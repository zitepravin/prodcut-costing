package com.automotive.costing.vendorselection.engine;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.vendorselection.entity.EvidenceStatus;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

/**
 * Guards evidence verification transitions.
 * PENDING is never treated as verified fact; EXPIRED may be re-verified after new evidence.
 */
@Component
public class EvidenceStatusTransitionEngine {

    public void assertTransitionAllowed(EvidenceStatus from, EvidenceStatus to) {
        if (to == null) {
            throw new BusinessRuleViolationException("Target evidence status is required", "VS-EV-01");
        }
        if (from == null || from == to) {
            return;
        }
        Set<EvidenceStatus> allowed = switch (from) {
            case PENDING -> EnumSet.of(
                    EvidenceStatus.VERIFIED,
                    EvidenceStatus.FAILED,
                    EvidenceStatus.WAIVED,
                    EvidenceStatus.NOT_REQUIRED,
                    EvidenceStatus.EXPIRED);
            case VERIFIED -> EnumSet.of(
                    EvidenceStatus.FAILED,
                    EvidenceStatus.EXPIRED,
                    EvidenceStatus.WAIVED);
            case FAILED -> EnumSet.of(
                    EvidenceStatus.VERIFIED,
                    EvidenceStatus.PENDING,
                    EvidenceStatus.WAIVED,
                    EvidenceStatus.EXPIRED);
            case EXPIRED -> EnumSet.of(
                    EvidenceStatus.VERIFIED,
                    EvidenceStatus.PENDING,
                    EvidenceStatus.FAILED,
                    EvidenceStatus.WAIVED);
            case WAIVED -> EnumSet.of(
                    EvidenceStatus.VERIFIED,
                    EvidenceStatus.EXPIRED,
                    EvidenceStatus.PENDING);
            case NOT_REQUIRED -> EnumSet.of(
                    EvidenceStatus.PENDING,
                    EvidenceStatus.VERIFIED,
                    EvidenceStatus.WAIVED);
        };
        if (!allowed.contains(to)) {
            throw new BusinessRuleViolationException(
                    "Evidence status transition not allowed: " + from + " → " + to, "VS-EV-02");
        }
    }
}
