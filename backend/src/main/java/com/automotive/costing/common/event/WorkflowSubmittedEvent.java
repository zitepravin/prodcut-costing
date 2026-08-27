package com.automotive.costing.common.event;

/** Published by {@code ApprovalWorkflowService.submit()}. See {@code WorkflowApprovedEvent}'s Javadoc for the decoupling rationale. */
public record WorkflowSubmittedEvent(String entityType, String entityId, String workflowInstanceId) {
}
