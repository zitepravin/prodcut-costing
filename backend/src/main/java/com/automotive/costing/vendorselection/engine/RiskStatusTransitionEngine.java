package com.automotive.costing.vendorselection.engine;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.vendorselection.entity.RiskStatus;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

/**
 * Risk register status transitions. ACCEPTED still requires mitigation rationale at service layer.
 */
@Component
public class RiskStatusTransitionEngine {

    public void assertTransitionAllowed(RiskStatus from, RiskStatus to) {
        if (to == null) {
            throw new BusinessRuleViolationException("Target risk status is required", "VS-RISK-10");
        }
        if (from == null || from == to) {
            return;
        }
        Set<RiskStatus> allowed = switch (from) {
            case OPEN -> EnumSet.of(RiskStatus.MITIGATING, RiskStatus.ACCEPTED, RiskStatus.CLOSED);
            case MITIGATING -> EnumSet.of(RiskStatus.OPEN, RiskStatus.ACCEPTED, RiskStatus.CLOSED);
            case ACCEPTED -> EnumSet.of(RiskStatus.CLOSED, RiskStatus.MITIGATING);
            case CLOSED -> EnumSet.of(RiskStatus.OPEN, RiskStatus.MITIGATING);
        };
        if (!allowed.contains(to)) {
            throw new BusinessRuleViolationException(
                    "Risk status transition not allowed: " + from + " → " + to, "VS-RISK-11");
        }
    }
}
