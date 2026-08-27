package com.automotive.costing.common.event;

/** Published by {@code ApprovalWorkflowService.decide()} when a step decision resolves the instance to REJECTED. See {@code WorkflowApprovedEvent}'s Javadoc for the decoupling rationale. */
public record WorkflowRejectedEvent(String entityType, String entityId, String workflowInstanceId) {
}
