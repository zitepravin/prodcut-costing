package com.automotive.costing.workflow.entity;

/** WF-05: "decision (approve/reject/return)" plus PENDING for a not-yet-decided step. */
public enum DecisionStatus {
    PENDING,
    APPROVED,
    REJECTED,
    RETURNED
}
