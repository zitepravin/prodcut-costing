package com.automotive.costing.fta.entity;
import jakarta.persistence.*; import lombok.*; import java.math.BigDecimal; import java.time.Instant; import java.time.LocalDate;
@Getter @Setter @NoArgsConstructor @Entity @Table(name="origin_rule")
public class OriginRule {
  @Id @Column(name="origin_rule_id",length=36) private String originRuleId;
  @Column(name="agreement_id",length=36,nullable=false) private String agreementId;
  @Column(name="hs_code",length=32,nullable=false) private String hsCode;
  @Column(name="rule_type",length=40,nullable=false) private String ruleType;
  @Column(name="rule_expression",length=500) private String ruleExpression;
  @Column(name="rvc_threshold_pct",precision=8,scale=4) private BigDecimal rvcThresholdPct;
  @Column(name="rvc_formula_code",length=40) private String rvcFormulaCode;
  @Column(name="ctc_level",length=10) private String ctcLevel;
  @Column(name="de_minimis_pct",precision=8,scale=4) private BigDecimal deMinimisPct;
  @Column(name="cumulation_allowed",nullable=false) private boolean cumulationAllowed;
  @Column(name="effective_from",nullable=false) private LocalDate effectiveFrom;
  @Column(name="effective_to") private LocalDate effectiveTo;
  @Column(name="source",length=200) private String source;
  @Column(name="version_no",nullable=false) private int versionNo=1;
  @Column(name="approval_status",length=20,nullable=false) private String approvalStatus="DRAFT";
  @Column(name="active",nullable=false) private boolean active=true;
  @Column(name="created_at",nullable=false) private Instant createdAt;
}
