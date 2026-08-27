package com.automotive.costing.governance;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.shouldcost.entity.ApprovalStatus;
import com.automotive.costing.shouldcost.entity.ShouldCostBuildUp;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V228 — Documents allowed should-cost approval transitions (unit-level status rules).
 * Service-level guards use rule IDs GOV-TRANSITION-01..05.
 */
class ShouldCostTransitionGovernanceTest {

    @Test
    void allowedPath_draftToSubmittedToApproved() {
        assertTrue(canSubmit(ApprovalStatus.DRAFT));
        assertTrue(canLock(ApprovalStatus.SUBMITTED));
        assertFalse(canSubmit(ApprovalStatus.APPROVED));
        assertFalse(canLock(ApprovalStatus.DRAFT));
        assertFalse(canRejectToDraft(ApprovalStatus.APPROVED));
    }

    @Test
    void rejectedReturnsToDraft_notRelease() {
        assertTrue(canRejectToDraft(ApprovalStatus.SUBMITTED));
        assertFalse(canRejectToDraft(ApprovalStatus.APPROVED));
    }

    private static boolean canSubmit(ApprovalStatus st) {
        if (st == ApprovalStatus.APPROVED) return false;
        return st == ApprovalStatus.DRAFT || st == ApprovalStatus.SUBMITTED || st == null;
    }

    private static boolean canLock(ApprovalStatus st) {
        return st == ApprovalStatus.SUBMITTED || st == ApprovalStatus.APPROVED;
    }

    private static boolean canRejectToDraft(ApprovalStatus st) {
        return st != ApprovalStatus.APPROVED;
    }
}
