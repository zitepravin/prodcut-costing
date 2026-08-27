package com.automotive.costing.workflow.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * WF-01/WF-02: one step in a {@link WorkflowDefinition}. {@code sequenceNumber} defines
 * ordering; two or more steps sharing the same {@code sequenceNumber} run in PARALLEL
 * (WF-01: "sequential and/or parallel approval steps") - see
 * {@code ApprovalWorkflowEngine}'s Javadoc for exactly how that's evaluated.
 * {@code minValue}/{@code maxValue}/{@code commodityFamily}/{@code plantCode} are WF-02's
 * routing rule ("route approval requests based on configurable rules including cost value
 * threshold, commodity, and plant") - all nullable, meaning "no constraint on this
 * dimension" (the same absence-means-unrestricted convention used for
 * {@code UserAccessScope} in M18).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "workflow_step_definition")
public class StepDefinition extends BaseAuditableEntity {

    @Id
    @Column(name = "step_definition_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String stepDefinitionId;

    @Column(name = "workflow_definition_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String workflowDefinitionId;

    @Column(name = "sequence_number", nullable = false)
    private int sequenceNumber;

    @Column(name = "approver_role", length = 50, nullable = false)
    private String approverRole;

    @Column(name = "min_value", precision = 18, scale = 6)
    private BigDecimal minValue;

    @Column(name = "max_value", precision = 18, scale = 6)
    private BigDecimal maxValue;

    @Column(name = "commodity_family", length = 100)
    private String commodityFamily;

    @Column(name = "plant_code", length = 50)
    private String plantCode;

    /** WF-04: hours before an unactioned step at this position escalates. Null = no auto-escalation configured. */
    @Column(name = "escalation_hours")
    private Integer escalationHours;
}
