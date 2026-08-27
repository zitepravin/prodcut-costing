package com.automotive.costing.costingsheet.entity;

/** Bases available for percentage overhead rules. */
public enum CostingSheetBaseCode {
    MATERIAL,
    PROCESS,
    TOOLING,
    PACKAGING,
    SUBTOTAL,
    TOTAL_SO_FAR,
    /** V277 extended bases */
    DIRECT_LABOR,
    MACHINE,
    PRIME_COST,
    MANUFACTURING_COST,
    TOTAL_DIRECT_COST
}
