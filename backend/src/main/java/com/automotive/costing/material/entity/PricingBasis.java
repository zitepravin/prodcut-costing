package com.automotive.costing.material.entity;

/**
 * How a material's unit rate was resolved for a given {@code MaterialCostCalculation}
 * (MAT-02). FIXED reads {@code MaterialMaster.rate} directly (the standard contract
 * price maintained by M01). INDEX_LINKED computes basePrice + commodity index value
 * (at the calculation's effective date) + alloy surcharge, per SRS 4.3.3's example
 * formula ("base price + LME aluminum index + alloy surcharge").
 */
public enum PricingBasis {
    FIXED,
    INDEX_LINKED
}
