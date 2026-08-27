package com.automotive.costing.vendorselection.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "vendor_question")
public class VendorQuestion extends BaseAuditableEntity {
    @Id
    @Column(name = "question_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String questionId;

    @Column(name = "customer_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;
    @Column(name = "question_code", length = 50, nullable = false)
    private String questionCode;
    @Column(name = "version_no", nullable = false)
    private int versionNo = 1;
    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 40, nullable = false)
    private QuestionCategory category;
    @Column(name = "question_text", length = 1000, nullable = false)
    private String questionText;
    @Enumerated(EnumType.STRING)
    @Column(name = "answer_type", length = 30, nullable = false)
    private QuestionAnswerType answerType;
    @Column(name = "required_flag", nullable = false)
    private boolean requiredFlag = true;
    @Column(name = "evidence_required", nullable = false)
    private boolean evidenceRequired;
    @Column(name = "mandatory_gate", nullable = false)
    private boolean mandatoryGate;
    @Column(name = "weight", precision = 9, scale = 4)
    private BigDecimal weight;
    @Column(name = "scoring_rule", length = 500)
    private String scoringRule;
    @Column(name = "active_flag", nullable = false)
    private boolean activeFlag = true;
    @Column(name = "effective_from")
    private LocalDate effectiveFrom;
    @Column(name = "effective_to")
    private LocalDate effectiveTo;
}
