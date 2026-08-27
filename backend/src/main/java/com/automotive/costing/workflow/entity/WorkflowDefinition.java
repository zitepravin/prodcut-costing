package com.automotive.costing.workflow.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * M14 Quotation &amp; Costing Approval Workflow. WF-01: "configuration of multi-step
 * approval workflows with sequential and/or parallel approval steps." A definition is a
 * reusable template - {@code entityType} names which kind of record it applies to (e.g.
 * "ShouldCostBuildUp" - the only real "cost record" type that exists in this system so
 * far; TDD's fuller {@code cost_record} concept spanning should-cost/supplier-quote/target
 * doesn't exist as a single entity yet, so this is deliberately a plain string rather than
 * an enum, so a new entity type doesn't require a schema change here later).
 * {@code StepDefinition} rows (WF-02) determine which definition applies to a given
 * submission via value/commodity/plant routing rules.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "workflow_definition")
public class WorkflowDefinition extends BaseAuditableEntity {

    @Id
    @Column(name = "workflow_definition_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String workflowDefinitionId;

    @Column(name = "name", length = 200, nullable = false)
    private String name;

    @Column(name = "entity_type", length = 50, nullable = false)
    private String entityType;

    @Column(name = "active", nullable = false)
    private boolean active;
}
