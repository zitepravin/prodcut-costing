package com.automotive.costing.fta.entity;
import jakarta.persistence.*; import lombok.*; import java.math.BigDecimal; import java.time.Instant; import java.time.LocalDate;
@Getter @Setter @NoArgsConstructor @Entity @Table(name="origin_assessment")
public class OriginAssessment {
  @Id @Column(name="assessment_id",length=36) private String assessmentId;
  @Column(name="customer_id",length=36,nullable=false) private String customerId;
  @Column(name="agreement_id",length=36) private String agreementId;
  @Column(name="product_code",length=64) private String productCode;
  @Column(name="hs_code",length=32,nullable=false) private String hsCode;
  @Column(name="origin_country",length=2,nullable=false) private String originCountry;
  @Column(name="export_country",length=2,nullable=false) private String exportCountry;
  @Column(name="destination_country",length=2,nullable=false) private String destinationCountry;
  @Column(name="assessment_date",nullable=false) private LocalDate assessmentDate;
  @Column(name="result_code",length=20,nullable=false) private String resultCode;
  @Column(name="reason",length=500) private String reason;
  @Column(name="rvc_pct",precision=18,scale=8) private BigDecimal rvcPct;
  @Column(name="classification_result",length=20) private String classificationResult;
  @Column(name="cumulation_result",length=20) private String cumulationResult;
  @Column(name="proof_id",length=36) private String proofId;
  @Column(name="rule_version") private Integer ruleVersion;
  @Column(name="preferential_rate",precision=18,scale=8) private BigDecimal preferentialRate;
  @Column(name="normal_rate",precision=18,scale=8) private BigDecimal normalRate;
  @Column(name="duty_benefit",precision=18,scale=6) private BigDecimal dutyBenefit;
  @Column(name="lifecycle_status",length=20,nullable=false) private String lifecycleStatus="DRAFT";
  @Column(name="snapshot_json",columnDefinition="JSON") private String snapshotJson;
  @Column(name="created_at",nullable=false) private Instant createdAt;
  @Column(name="updated_at",nullable=false) private Instant updatedAt;
  @Column(name="approved_by",length=64) private String approvedBy;
  @Column(name="approved_at") private Instant approvedAt;
  @Column(name="locked_at") private Instant lockedAt;
}
