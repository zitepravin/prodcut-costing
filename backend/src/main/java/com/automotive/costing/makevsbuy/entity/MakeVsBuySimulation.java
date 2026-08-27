package com.automotive.costing.makevsbuy.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "make_vs_buy_simulation")
public class MakeVsBuySimulation extends BaseAuditableEntity {

    @Id
    @Column(name = "simulation_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String simulationId;

    @Column(name = "customer_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;

    @Column(name = "program_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String programId;

    @Column(name = "bom_node_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String bomNodeId;

    @Column(name = "label", length = 120, nullable = false)
    private String label;

    @Column(name = "status", length = 20, nullable = false)
    private String status = "DRAFT";

    @Column(name = "selected_build_up_id", length = 36, columnDefinition = "CHAR(36)")
    private String selectedBuildUpId;

    @Column(name = "mixed_cost_id", length = 36, columnDefinition = "CHAR(36)")
    private String mixedCostId;

    @Column(name = "recommendation_code", length = 20)
    private String recommendationCode;

    @Column(name = "recommendation_summary", length = 2000)
    private String recommendationSummary;

    @Column(name = "decision_code", length = 20)
    private String decisionCode;

    @Column(name = "decision_reason", length = 1000)
    private String decisionReason;

    @Column(name = "decided_by", length = 36)
    private String decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "bom_flag_applied", nullable = false)
    private boolean bomFlagApplied = false;

    @Column(name = "bom_flag_applied_at")
    private Instant bomFlagAppliedAt;

    @Column(name = "frozen_at")
    private Instant frozenAt;

    @Column(name = "reporting_currency", length = 3, columnDefinition = "CHAR(3)")
    private String reportingCurrency;

    @Column(name = "notes", length = 1000)
    private String notes;
}
