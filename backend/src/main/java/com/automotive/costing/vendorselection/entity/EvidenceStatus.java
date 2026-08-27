package com.automotive.costing.vendorselection.entity;

/** Evidence verification lifecycle — never treat PENDING as verified fact. */
public enum EvidenceStatus {
    PENDING,
    VERIFIED,
    FAILED,
    EXPIRED,
    NOT_REQUIRED,
    WAIVED
}
