package com.automotive.costing.notification.listener;

import com.automotive.costing.common.event.ApprovalEscalatedEvent;
import com.automotive.costing.common.event.WorkflowApprovedEvent;
import com.automotive.costing.common.event.WorkflowRejectedEvent;
import com.automotive.costing.common.event.WorkflowSubmittedEvent;
import com.automotive.costing.notification.entity.NotificationSeverity;
import com.automotive.costing.notification.entity.NotificationType;
import com.automotive.costing.notification.service.NotificationService;
import com.automotive.costing.platform.user.entity.User;
import com.automotive.costing.platform.user.repository.UserRepository;
import com.automotive.costing.workflow.entity.ApprovalStepInstance;
import com.automotive.costing.workflow.entity.DecisionStatus;
import com.automotive.costing.workflow.entity.WorkflowInstance;
import com.automotive.costing.workflow.repository.ApprovalStepInstanceRepository;
import com.automotive.costing.workflow.repository.WorkflowInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Translates workflow domain events into in-app notifications for relevant users.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WorkflowNotificationListener {

    private final NotificationService notificationService;
    private final WorkflowInstanceRepository workflowInstanceRepository;
    private final ApprovalStepInstanceRepository stepInstanceRepository;
    private final UserRepository userRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSubmitted(WorkflowSubmittedEvent event) {
        workflowInstanceRepository.findById(event.workflowInstanceId()).ifPresent(instance -> {
            // Notify users holding the first pending step's role
            List<ApprovalStepInstance> steps = stepInstanceRepository
                    .findByWorkflowInstanceIdOrderBySequenceNumberAsc(instance.getWorkflowInstanceId());
            steps.stream()
                    .filter(s -> s.getDecision() == DecisionStatus.PENDING)
                    .findFirst()
                    .ifPresent(step -> notifyRole(
                            step.getApproverRole(),
                            NotificationType.WORKFLOW_SUBMITTED,
                            "Approval required",
                            "A " + event.entityType() + " record was submitted for approval (instance "
                                    + event.workflowInstanceId() + ").",
                            event.entityType(),
                            event.entityId(),
                            NotificationSeverity.INFO));
        });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onApproved(WorkflowApprovedEvent event) {
        workflowInstanceRepository.findById(event.workflowInstanceId()).ifPresent(instance -> {
            if (instance.getSubmittedBy() != null) {
                notificationService.publish(
                        instance.getSubmittedBy(),
                        NotificationType.WORKFLOW_APPROVED,
                        "Workflow approved",
                        event.entityType() + " " + event.entityId() + " was fully approved.",
                        event.entityType(),
                        event.entityId(),
                        NotificationSeverity.INFO);
            }
        });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRejected(WorkflowRejectedEvent event) {
        workflowInstanceRepository.findById(event.workflowInstanceId()).ifPresent(instance -> {
            if (instance.getSubmittedBy() != null) {
                notificationService.publish(
                        instance.getSubmittedBy(),
                        NotificationType.WORKFLOW_REJECTED,
                        "Workflow rejected",
                        event.entityType() + " " + event.entityId() + " was rejected.",
                        event.entityType(),
                        event.entityId(),
                        NotificationSeverity.WARNING);
            }
        });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEscalated(ApprovalEscalatedEvent event) {
        if (event.newApproverUserId() != null) {
            notificationService.publish(
                    event.newApproverUserId(),
                    NotificationType.APPROVAL_ESCALATED,
                    "Approval escalated to you",
                    "Step " + event.stepInstanceId() + " (role " + event.approverRole()
                            + ") is overdue and was reassigned to you.",
                    "ApprovalStepInstance",
                    event.stepInstanceId(),
                    NotificationSeverity.CRITICAL);
        }
        // Also notify original role holders as awareness if no specific assignee
        if (event.newApproverUserId() == null) {
            notifyRole(
                    event.approverRole(),
                    NotificationType.APPROVAL_ESCALATED,
                    "Approval overdue",
                    "Step " + event.stepInstanceId() + " is overdue and marked escalated.",
                    "ApprovalStepInstance",
                    event.stepInstanceId(),
                    NotificationSeverity.WARNING);
        }
    }

    private void notifyRole(
            String roleCode,
            NotificationType type,
            String title,
            String body,
            String entityType,
            String entityId,
            NotificationSeverity severity) {
        List<User> users = userRepository.findActiveByRoleCode(roleCode);
        Set<String> seen = new HashSet<>();
        for (User u : users) {
            if (seen.add(u.getUserId())) {
                notificationService.publish(u.getUserId(), type, title, body, entityType, entityId, severity);
            }
        }
        if (users.isEmpty()) {
            log.debug("No active users for role {} — skipped notification '{}'", roleCode, title);
        }
    }
}
