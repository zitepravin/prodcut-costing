package com.automotive.costing.vendorselection.entity;

/**
 * Qualification lifecycle — distinct from operational ACTIVE/INACTIVE/BLOCKED on supplier_master.
 * Final APPROVED requires human action; system never auto-approves.
 */
public enum SupplierLifecycleStatus {
    DISCOVERED,
    IMPORTED,
    UNDER_VERIFICATION,
    QUALIFIED,
    EVALUATED,
    RECOMMENDED,
    APPROVAL_PENDING,
    APPROVED,
    REJECTED,
    BLOCKED,
    SUSPENDED
}
