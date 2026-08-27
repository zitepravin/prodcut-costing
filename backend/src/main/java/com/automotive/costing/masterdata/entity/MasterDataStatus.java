package com.automotive.costing.masterdata.entity;

/**
 * Shared lifecycle status for every master data record (material/machine/labor/overhead).
 * MDM-05: a record with missing mandatory attributes cannot move out of PENDING_APPROVAL.
 * Deactivation (not deletion) is the only path to retiring a record referenced by an
 * approved cost estimate, per SRS Section 4.1.5 business rule.
 */
public enum MasterDataStatus {
    ACTIVE,
    INACTIVE,
    PENDING_APPROVAL
}
