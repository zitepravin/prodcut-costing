package com.automotive.costing.process.entity;

/**
 * SRS 4.4.5 business rule: "A process routing cannot be marked 'Approved' if any
 * operation is missing a machine or labor assignment." DRAFT is the only status new
 * routings start in; {@code ProcessRoutingService.approve()} is the sole transition to
 * APPROVED and enforces that rule before allowing it.
 */
public enum RoutingStatus {
    DRAFT,
    APPROVED
}
