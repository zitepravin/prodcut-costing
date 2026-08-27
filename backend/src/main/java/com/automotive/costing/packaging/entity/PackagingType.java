package com.automotive.costing.packaging.entity;

/**
 * Primary packaging method for a part (M07). Used for reporting and rate selection;
 * cost arithmetic is driven by the numeric fields on the profile, not by this enum alone.
 */
public enum PackagingType {
    EXPENDABLE_CARTON,
    RETURNABLE_CONTAINER,
    BLISTER_TRAY,
    PALLETIZED,
    BULK_BIN,
    PROTECTIVE_WRAP,
    CUSTOM
}
