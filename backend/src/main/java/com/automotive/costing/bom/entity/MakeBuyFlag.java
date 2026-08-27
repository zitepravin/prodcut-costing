package com.automotive.costing.bom.entity;

/**
 * BOM-07: drives which downstream costing module supplies a part's unit cost.
 * MAKE -> internal Process/Labor Costing (M04); BUY -> Supplier Costing (M10).
 */
public enum MakeBuyFlag {
    MAKE,
    BUY
}
