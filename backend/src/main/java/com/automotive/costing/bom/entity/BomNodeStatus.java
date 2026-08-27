package com.automotive.costing.bom.entity;

/** BOM-03 business rule: cannot move to RELEASED without a linked ECN reference. */
public enum BomNodeStatus {
    DRAFT,
    RELEASED
}
