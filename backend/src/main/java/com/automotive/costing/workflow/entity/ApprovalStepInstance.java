package com.automotive.costing.workflow.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * WF-03/WF-05: one approver's actual step within a {@link WorkflowInstance}, copied from
 * the matching {@link StepDefinition} at submission time (so a later edit to the
 * definition template never silently changes an in-flight instance's requirements).
 * WF-05: "capture approver identity, timestamp, decision (approve/reject/return), and
 * comment for every workflow step" - once {@code decision} moves off PENDING, this row is
 * never updated again (WF-07's immutability requirement); a correction happens via a new
 * workflow instance after the underlying record returns to Draft (SRS 4.14.5), not by
 * editing this row.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "approval_step_instance")
public class ApprovalStepInstance extends BaseAuditableEntity {

    @Id
    @Column(name = "step_instance_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String stepInstanceId;

    @Column(name = "workflow_instance_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String workflowInstanceId;

    @Column(name = "sequence_number", nullable = false)
    private int sequenceNumber;

    @Column(name = "approver_role", length = 50, nullable = false)
    private String approverRole;

    @Column(name = "approver_user_id", length = 36, columnDefinition = "CHAR(36)")
    private String approverUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", length = 20, nullable = false)
    private DecisionStatus decision;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "comment", length = 1000)
    private String comment;

    @Column(name = "due_at")
    private Instant dueAt;

    @Column(name = "escalated", nullable = false)
    private boolean escalated;

    /** SRS 4.14.5: "An approval step cannot be skipped or bypassed except by a user holding an explicit 'Approval Override' permission, and any override must be logged with justification." */
    @Column(name = "override_flag", nullable = false)
    private boolean overrideFlag;

    @Column(name = "override_justification", length = 1000)
    private String overrideJustification;
}
