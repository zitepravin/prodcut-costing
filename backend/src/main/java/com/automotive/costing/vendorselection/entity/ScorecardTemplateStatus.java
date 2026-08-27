package com.automotive.costing.vendorselection.entity;

/**
 * P3-07a lifecycle:
 * DRAFT — editable, not usable for new evaluations
 * ACTIVE — published; usable for new evaluations (historical results stay bound to template id)
 * INACTIVE — retired; not usable for new evaluations
 */
public enum ScorecardTemplateStatus {
    DRAFT,
    ACTIVE,
    INACTIVE
}
