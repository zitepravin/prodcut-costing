package com.automotive.costing.vendorselection.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "vendor_questionnaire_evaluation")
public class VendorQuestionnaireEvaluation extends BaseAuditableEntity {

    @Id
    @Column(name = "evaluation_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String evaluationId;

    @Column(name = "customer_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;

    @Column(name = "supplier_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String supplierId;

    @Column(name = "overall_eligible", nullable = false)
    private boolean overallEligible;

    @Column(name = "mandatory_gates_passed", nullable = false)
    private boolean mandatoryGatesPassed;

    @Column(name = "required_answered", nullable = false)
    private boolean requiredAnswered;

    @Column(name = "total_active_questions", nullable = false)
    private int totalActiveQuestions;

    @Column(name = "answered_count", nullable = false)
    private int answeredCount;

    @Column(name = "mandatory_gate_count", nullable = false)
    private int mandatoryGateCount;

    @Column(name = "mandatory_gate_failed", nullable = false)
    private int mandatoryGateFailed;

    @Column(name = "evidence_pending_count", nullable = false)
    private int evidencePendingCount;

    @Column(name = "weighted_score", precision = 9, scale = 4)
    private BigDecimal weightedScore;

    @Column(name = "max_possible_score", precision = 9, scale = 4)
    private BigDecimal maxPossibleScore;

    @Column(name = "failed_gate_codes", length = 1000)
    private String failedGateCodes;

    @Column(name = "missing_question_codes", length = 1000)
    private String missingQuestionCodes;

    @Lob
    @Column(name = "evaluation_json", columnDefinition = "LONGTEXT")
    private String evaluationJson;

    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt;

    @Column(name = "evaluated_by", length = 36, columnDefinition = "CHAR(36)")
    private String evaluatedBy;

    @Column(name = "notes", length = 1000)
    private String notes;
}
