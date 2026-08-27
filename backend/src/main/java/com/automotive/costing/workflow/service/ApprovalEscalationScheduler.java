package com.automotive.costing.workflow.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.event.ApprovalEscalatedEvent;
import com.automotive.costing.platform.user.entity.User;
import com.automotive.costing.platform.user.repository.UserRepository;
import com.automotive.costing.workflow.entity.ApprovalStepInstance;
import com.automotive.costing.workflow.entity.DecisionStatus;
import com.automotive.costing.workflow.repository.ApprovalStepInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * WF-04: escalate overdue approvals to a designated backup or manager after the
 * configured threshold ({@code dueAt} on the step, derived from step-definition
 * {@code escalationHours}).
 *
 * <p>Detection: mark {@code escalated=true}. Action: reassign {@code approverUserId}
 * to an active user holding the mapped escalation role (manager/admin backup chain).
 * Notification delivery (email/in-app) remains out of scope until a notification module exists.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApprovalEscalationScheduler {

    /**
     * Role → backup/manager role for reassignment. Unlisted roles escalate to
     * COSTING_MANAGER then SYSTEM_ADMINISTRATOR.
     */
    private static final Map<String, List<String>> ESCALATION_ROLE_CHAIN = Map.of(
            "COSTING_ENGINEER", List.of("COSTING_MANAGER", "PROGRAM_COST_MANAGER", "SYSTEM_ADMINISTRATOR"),
            "COSTING_MANAGER", List.of("PROGRAM_COST_MANAGER", "SYSTEM_ADMINISTRATOR"),
            "PROGRAM_COST_MANAGER", List.of("SYSTEM_ADMINISTRATOR"),
            "PURCHASING_BUYER", List.of("PURCHASING_MANAGER", "SOURCING_LEAD", "SYSTEM_ADMINISTRATOR"),
            "PURCHASING_MANAGER", List.of("SOURCING_LEAD", "SYSTEM_ADMINISTRATOR"),
            "SOURCING_LEAD", List.of("SYSTEM_ADMINISTRATOR"),
            "PROCESS_ENGINEER", List.of("COSTING_MANAGER", "SYSTEM_ADMINISTRATOR"),
            "TOOLING_ENGINEER", List.of("COSTING_MANAGER", "SYSTEM_ADMINISTRATOR"),
            "FINANCE_CONTROLLER", List.of("SYSTEM_ADMINISTRATOR"),
            "MASTER_DATA_ADMIN", List.of("SYSTEM_ADMINISTRATOR")
    );

    private final ApprovalStepInstanceRepository stepInstanceRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(fixedRate = 15 * 60 * 1000)
    @Transactional
    public void escalateOverdueSteps() {
        List<ApprovalStepInstance> overdue = stepInstanceRepository
                .findByDecisionAndEscalatedFalseAndDueAtBefore(DecisionStatus.PENDING, Instant.now());
        if (overdue.isEmpty()) {
            return;
        }

        log.info("WF-04: {} approval step(s) overdue — escalating.", overdue.size());
        for (ApprovalStepInstance step : overdue) {
            step.setEscalated(true);
            String previousAssignee = step.getApproverUserId();
            String previousRole = step.getApproverRole();

            User backup = resolveBackupApprover(step.getApproverRole(), previousAssignee);
            if (backup != null) {
                step.setApproverUserId(backup.getUserId());
                // Keep original role for auditability of the definition; assignment drives inbox visibility.
                log.info("WF-04: step {} reassigned to user {} ({})",
                        step.getStepInstanceId(), backup.getUserId(), backup.getUsername());
            } else {
                log.warn("WF-04: step {} marked escalated but no active backup user found for role {}",
                        step.getStepInstanceId(), previousRole);
            }

            stepInstanceRepository.save(step);
            auditService.recordUpdate(
                    "ApprovalStepInstance",
                    step.getStepInstanceId(),
                    null,
                    "escalated=true; previousAssignee=" + previousAssignee
                            + "; previousRole=" + previousRole
                            + "; newAssignee=" + step.getApproverUserId()
                            + "; dueAt=" + step.getDueAt());
            eventPublisher.publishEvent(new ApprovalEscalatedEvent(
                    step.getStepInstanceId(),
                    step.getWorkflowInstanceId(),
                    previousAssignee,
                    step.getApproverUserId(),
                    previousRole));
        }
    }

    private User resolveBackupApprover(String currentRole, String excludeUserId) {
        List<String> chain = ESCALATION_ROLE_CHAIN.getOrDefault(
                currentRole,
                List.of("COSTING_MANAGER", "SYSTEM_ADMINISTRATOR"));
        for (String roleCode : chain) {
            List<User> candidates = userRepository.findActiveByRoleCode(roleCode);
            for (User candidate : candidates) {
                if (excludeUserId == null || !excludeUserId.equals(candidate.getUserId())) {
                    return candidate;
                }
            }
        }
        return null;
    }
}
