package com.automotive.costing.common.event;

/**
 * Published by {@code ApprovalWorkflowService} when every step in a workflow instance is
 * APPROVED. Mirrors {@code MaterialRatePublishedEvent}'s rationale exactly: M14 (workflow)
 * has no compile-time dependency on whichever module owns {@code entityType} (today, only
 * "ShouldCostBuildUp" from M08) - a listener in that module reacts instead. This is the
 * seam a future M08 change (e.g. the BomNodeService/ShouldCostBuildUp reconciliation
 * flagged in NEXT_AGENT.md) or a new entity type (e.g. once M08's should-cost record
 * evolves toward the TDD's fuller cost_record shape) would plug into without M14 itself
 * changing.
 *
 * @param entityType matches {@code WorkflowDefinition.entityType} / {@code WorkflowInstance.entityType}
 * @param entityId   the approved record's ID within that entity type
 * @param workflowInstanceId the completed instance, for traceability
 */
public record WorkflowApprovedEvent(String entityType, String entityId, String workflowInstanceId) {
}
