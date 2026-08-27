package com.automotive.costing.common.event;

/**
 * Published by {@code ApprovalEscalationScheduler} after an overdue step is escalated
 * and optionally reassigned. Notification module listens without coupling M14 to UI push.
 */
public record ApprovalEscalatedEvent(
        String stepInstanceId,
        String workflowInstanceId,
        String previousApproverUserId,
        String newApproverUserId,
        String approverRole
) {
}
