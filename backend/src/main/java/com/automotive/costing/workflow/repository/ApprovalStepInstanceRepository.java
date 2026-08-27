package com.automotive.costing.workflow.repository;

import com.automotive.costing.workflow.entity.ApprovalStepInstance;
import com.automotive.costing.workflow.entity.DecisionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ApprovalStepInstanceRepository extends JpaRepository<ApprovalStepInstance, String> {

    List<ApprovalStepInstance> findByWorkflowInstanceIdOrderBySequenceNumberAsc(String workflowInstanceId);

    /** WF-03: pending steps matching any of the user's roles. */
    List<ApprovalStepInstance> findByApproverRoleInAndDecision(List<String> approverRoles, DecisionStatus decision);

    /**
     * WF-04: steps explicitly assigned to a user after escalation (approverUserId set while still PENDING).
     */
    List<ApprovalStepInstance> findByApproverUserIdAndDecision(String approverUserId, DecisionStatus decision);

    /** WF-04: overdue, not-yet-escalated, still-pending steps. */
    List<ApprovalStepInstance> findByDecisionAndEscalatedFalseAndDueAtBefore(DecisionStatus decision, Instant cutoff);

    long countByDecision(DecisionStatus decision);
}
