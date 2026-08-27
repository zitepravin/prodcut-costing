package com.automotive.costing.vendorselection.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "supplier_eligibility_result")
public class SupplierEligibilityResult extends BaseAuditableEntity {

    @Id
    @Column(name = "result_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String resultId;

    @Column(name = "supplier_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String supplierId;

    @Column(name = "bom_node_id", length = 36, columnDefinition = "CHAR(36)")
    private String bomNodeId;

    @Column(name = "program_id", length = 36, columnDefinition = "CHAR(36)")
    private String programId;

    @Column(name = "overall_eligible", nullable = false)
    private boolean overallEligible;

    @Column(name = "failed_gates", length = 500)
    private String failedGates;

    @Lob
    @Column(name = "evaluation_json", columnDefinition = "LONGTEXT")
    private String evaluationJson;

    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt;

    @Column(name = "evaluated_by", length = 36, columnDefinition = "CHAR(36)")
    private String evaluatedBy;

    /** V46: optional link to questionnaire evaluation snapshot used for QUESTIONNAIRE gate. */
    @Column(name = "questionnaire_evaluation_id", length = 36, columnDefinition = "CHAR(36)")
    private String questionnaireEvaluationId;

    @Column(name = "require_questionnaire", nullable = false)
    private boolean requireQuestionnaire = false;

    @Column(name = "questionnaire_eligible")
    private Boolean questionnaireEligible;

    @Column(name = "require_no_critical_open_risks", nullable = false)
    private boolean requireNoCriticalOpenRisks = false;

    @Column(name = "require_no_high_open_risks", nullable = false)
    private boolean requireNoHighOpenRisks = false;

    @Column(name = "open_critical_risk_count")
    private Integer openCriticalRiskCount;

    @Column(name = "open_high_risk_count")
    private Integer openHighRiskCount;

    @Column(name = "open_risk_count")
    private Integer openRiskCount;

    @Column(name = "lifecycle_status", length = 30)
    private String lifecycleStatus;

    @Column(name = "lifecycle_blocked", nullable = false)
    private boolean lifecycleBlocked = false;
}
