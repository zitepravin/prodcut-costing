package com.automotive.costing.vendorselection.engine;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.vendorselection.entity.WarrantyClaimStatus;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

/**
 * Post-create warranty claim status transitions.
 * Evidence verification remains independent of claim commercial status.
 */
@Component
public class WarrantyClaimStatusTransitionEngine {

    public void assertTransitionAllowed(WarrantyClaimStatus from, WarrantyClaimStatus to) {
        if (to == null) {
            throw new BusinessRuleViolationException("Target claim status is required", "VS-WC-10");
        }
        if (from == null) {
            return;
        }
        if (from == to) {
            throw new BusinessRuleViolationException("Claim status is already " + to, "VS-WC-11");
        }
        Set<WarrantyClaimStatus> allowed = switch (from) {
            case OPEN -> EnumSet.of(
                    WarrantyClaimStatus.ACCEPTED,
                    WarrantyClaimStatus.REJECTED,
                    WarrantyClaimStatus.WITHDRAWN,
                    WarrantyClaimStatus.CLOSED);
            case ACCEPTED -> EnumSet.of(WarrantyClaimStatus.CLOSED);
            case REJECTED -> EnumSet.of(WarrantyClaimStatus.CLOSED, WarrantyClaimStatus.OPEN);
            case WITHDRAWN -> EnumSet.of(WarrantyClaimStatus.CLOSED);
            case CLOSED -> EnumSet.noneOf(WarrantyClaimStatus.class);
        };
        if (!allowed.contains(to)) {
            throw new BusinessRuleViolationException(
                    "Claim status transition not allowed: " + from + " → " + to, "VS-WC-12");
        }
    }
}
