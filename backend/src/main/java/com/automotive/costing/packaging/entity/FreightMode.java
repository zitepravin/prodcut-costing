package com.automotive.costing.packaging.entity;

/**
 * Direction of freight cost applied in packaging & logistics costing (M07).
 * BOTH applies the freight rate once for each direction (2x unit freight).
 */
public enum FreightMode {
    NONE,
    INBOUND,
    OUTBOUND,
    BOTH
}
