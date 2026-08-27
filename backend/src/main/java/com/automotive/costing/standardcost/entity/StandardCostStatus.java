package com.automotive.costing.standardcost.entity;

/**
 * C1 lifecycle for a standard-cost object (distinct from should-cost draft).
 * DRAFT → MARKED_FOR_RELEASE → RELEASED → FROZEN → SUPERSEDED
 */
public enum StandardCostStatus {
    DRAFT,
    MARKED_FOR_RELEASE,
    RELEASED,
    FROZEN,
    SUPERSEDED
}
