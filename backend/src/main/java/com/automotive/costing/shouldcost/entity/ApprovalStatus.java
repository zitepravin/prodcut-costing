package com.automotive.costing.shouldcost.entity;

/**
 * WF-06/SRS 4.14.5: "A rejected cost record returns to 'Draft' status and must restart
 * the workflow from the beginning after correction." This is the status ShouldCostBuildUp
 * was missing at the end of the M14 session - without it, a REJECTED workflow outcome had
 * nowhere to route the underlying record back to. Values follow the TDD's cost_record
 * status vocabulary (TDD Section 4.3: "Draft/Submitted/Approved/Rejected") even though
 * ShouldCostBuildUp is not literally the TDD's cost_record entity - see this class's
 * sibling entity's Javadoc for why a single unifying cost_record doesn't exist yet.
 */
public enum ApprovalStatus {
    DRAFT,
    SUBMITTED,
    APPROVED,
    REJECTED
}
