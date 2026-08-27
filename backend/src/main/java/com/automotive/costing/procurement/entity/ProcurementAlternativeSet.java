package com.automotive.costing.procurement.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "procurement_alternative_set")
public class ProcurementAlternativeSet extends BaseAuditableEntity {

    @Id
    @Column(name = "set_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String setId;

    @Column(name = "bom_node_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String bomNodeId;

    @Column(name = "program_id", length = 36, columnDefinition = "CHAR(36)")
    private String programId;

    @Column(name = "label", length = 200, nullable = false)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ProcurementSetStatus status = ProcurementSetStatus.DRAFT;

    @Column(name = "recommendation_code", length = 40)
    private String recommendationCode;

    @Column(name = "recommendation_summary", length = 2000)
    private String recommendationSummary;

    @Column(name = "decision_code", length = 40)
    private String decisionCode;

    @Column(name = "decision_reason", length = 1000)
    private String decisionReason;

    @Column(name = "decided_by", length = 36)
    private String decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;
}
