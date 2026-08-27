package com.automotive.costing.tooling.entity;

/**
 * TL-05: "flag tooling ownership (Customer-owned vs. Supplier-owned) and adjust
 * piece-price presentation accordingly." Business rule (SRS 4.6.5): "Customer-owned
 * tooling cost is excluded from supplier piece-price but tracked separately as a capital
 * line item."
 */
public enum ToolingOwnership {
    CUSTOMER,
    SUPPLIER
}
