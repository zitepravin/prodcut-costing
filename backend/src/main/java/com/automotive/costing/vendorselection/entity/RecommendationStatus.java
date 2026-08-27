package com.automotive.costing.vendorselection.entity;

/**
 * Human-controlled lifecycle. System never transitions to APPROVED without explicit human decision.
 */
public enum RecommendationStatus {
    DRAFT,
    SUBMITTED,
    APPROVED,
    REJECTED,
    OVERRIDDEN
}
