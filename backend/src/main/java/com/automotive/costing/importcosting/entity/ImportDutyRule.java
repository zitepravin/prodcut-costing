package com.automotive.costing.importcosting.entity;
import jakarta.persistence.*; import lombok.*; import java.math.BigDecimal; import java.time.Instant; import java.time.LocalDate;
@Getter @Setter @NoArgsConstructor @Entity @Table(name="import_duty_rule")
public class ImportDutyRule {
  @Id @Column(name="duty_rule_id",length=36,nullable=false) private String dutyRuleId;
  @Column(name="customer_id",length=36,nullable=false) private String customerId;
  @Column(name="hs_code",length=32,nullable=false) private String hsCode;
  @Column(name="origin_country",length=2,nullable=false) private String originCountry;
  @Column(name="destination_country",length=2,nullable=false) private String destinationCountry;
  @Column(name="duty_type",length=20,nullable=false) private String dutyType;
  @Column(name="ad_valorem_rate",precision=18,scale=8) private BigDecimal adValoremRate;
  @Column(name="specific_amount",precision=18,scale=6) private BigDecimal specificAmount;
  @Column(name="specific_currency",length=3) private String specificCurrency;
  @Column(name="taxable_base",length=40,nullable=false) private String taxableBase="CUSTOMS_VALUE";
  @Column(name="effective_from",nullable=false) private LocalDate effectiveFrom;
  @Column(name="effective_to") private LocalDate effectiveTo;
  @Column(name="source",length=100) private String source;
  @Column(name="version_no",nullable=false) private int versionNo=1;
  @Column(name="approval_status",length=20,nullable=false) private String approvalStatus="DRAFT";
  @Column(name="active",nullable=false) private boolean active=true;
  @Column(name="created_at",nullable=false) private Instant createdAt;
  @Column(name="updated_at",nullable=false) private Instant updatedAt;
}
