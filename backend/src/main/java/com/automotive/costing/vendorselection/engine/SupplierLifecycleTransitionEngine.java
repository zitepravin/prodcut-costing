package com.automotive.costing.vendorselection.engine;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.vendorselection.entity.SupplierLifecycleStatus;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

/**
 * Allowed qualification transitions. APPROVED only from APPROVAL_PENDING (human gate).
 */
@Component
public class SupplierLifecycleTransitionEngine {

    public void assertTransitionAllowed(SupplierLifecycleStatus from, SupplierLifecycleStatus to) {
        if (to == null) {
            throw new BusinessRuleViolationException("Target lifecycle status is required", "VS-LC-01");
        }
        if (from == null) {
            return;
        }
        if (from == to) {
            throw new BusinessRuleViolationException("Status is already " + to, "VS-LC-02");
        }
        Set<SupplierLifecycleStatus> allowed = switch (from) {
            case DISCOVERED -> EnumSet.of(SupplierLifecycleStatus.IMPORTED,
                    SupplierLifecycleStatus.UNDER_VERIFICATION, SupplierLifecycleStatus.REJECTED,
                    SupplierLifecycleStatus.BLOCKED);
            case IMPORTED -> EnumSet.of(SupplierLifecycleStatus.UNDER_VERIFICATION,
                    SupplierLifecycleStatus.REJECTED, SupplierLifecycleStatus.BLOCKED);
            case UNDER_VERIFICATION -> EnumSet.of(SupplierLifecycleStatus.QUALIFIED,
                    SupplierLifecycleStatus.REJECTED, SupplierLifecycleStatus.BLOCKED);
            case QUALIFIED -> EnumSet.of(SupplierLifecycleStatus.EVALUATED,
                    SupplierLifecycleStatus.SUSPENDED, SupplierLifecycleStatus.BLOCKED);
            case EVALUATED -> EnumSet.of(SupplierLifecycleStatus.RECOMMENDED,
                    SupplierLifecycleStatus.APPROVAL_PENDING, SupplierLifecycleStatus.SUSPENDED,
                    SupplierLifecycleStatus.BLOCKED);
            case RECOMMENDED -> EnumSet.of(SupplierLifecycleStatus.APPROVAL_PENDING,
                    SupplierLifecycleStatus.REJECTED, SupplierLifecycleStatus.SUSPENDED);
            case APPROVAL_PENDING -> EnumSet.of(SupplierLifecycleStatus.APPROVED,
                    SupplierLifecycleStatus.REJECTED);
            case APPROVED -> EnumSet.of(SupplierLifecycleStatus.SUSPENDED, SupplierLifecycleStatus.BLOCKED);
            case REJECTED -> EnumSet.of(SupplierLifecycleStatus.UNDER_VERIFICATION,
                    SupplierLifecycleStatus.BLOCKED);
            case BLOCKED -> EnumSet.of(SupplierLifecycleStatus.UNDER_VERIFICATION);
            case SUSPENDED -> EnumSet.of(SupplierLifecycleStatus.QUALIFIED,
                    SupplierLifecycleStatus.APPROVED, SupplierLifecycleStatus.BLOCKED);
        };
        if (!allowed.contains(to)) {
            throw new BusinessRuleViolationException(
                    "Transition not allowed: " + from + " → " + to, "VS-LC-03");
        }
    }
}
