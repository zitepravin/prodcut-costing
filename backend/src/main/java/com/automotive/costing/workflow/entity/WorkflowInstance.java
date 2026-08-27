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
 * WF-01/WF-06/WF-07: one running (or completed) approval process for a specific record.
 * {@code entityType}/{@code entityId} is a generic reference (e.g. "ShouldCostBuildUp" /
 * a {@code ShouldCostBuildUp.buildUpId}) rather than a FK to one specific table - the same
 * reasoning as {@link WorkflowDefinition#getEntityType()}. WF-07: "retain full, immutable
 * approval history viewable from the cost record at any time" is satisfied by never
 * updating {@link com.automotive.costing.workflow.entity.ApprovalStepInstance} rows after
 * they're decided (see that class) plus this instance row itself only ever having its
 * {@code status}/{@code completedAt} set once, at completion.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "approval_workflow_instance")
public class WorkflowInstance extends BaseAuditableEntity {

    @Id
    @Column(name = "workflow_instance_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String workflowInstanceId;

    @Column(name = "customer_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;

    @Column(name = "workflow_definition_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String workflowDefinitionId;

    @Column(name = "entity_type", length = 50, nullable = false)
    private String entityType;

    @Column(name = "entity_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private WorkflowInstanceStatus status;

    @Column(name = "submitted_by", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String submittedBy;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}
