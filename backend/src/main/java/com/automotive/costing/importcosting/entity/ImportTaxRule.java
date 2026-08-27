package com.automotive.costing.importcosting.entity;
import jakarta.persistence.*; import lombok.*; import java.math.BigDecimal; import java.time.Instant; import java.time.LocalDate;
@Getter @Setter @NoArgsConstructor @Entity @Table(name="import_tax_rule")
public class ImportTaxRule {
  @Id @Column(name="tax_rule_id",length=36,nullable=false) private String taxRuleId;
  @Column(name="customer_id",length=36,nullable=false) private String customerId;
  @Column(name="tax_code",length=40,nullable=false) private String taxCode;
  @Column(name="tax_type",length=40,nullable=false) private String taxType;
  @Column(name="rate",precision=18,scale=8,nullable=false) private BigDecimal rate;
  @Column(name="tax_base",length=40,nullable=false) private String taxBase="CUSTOMS_VALUE_PLUS_DUTY";
  @Column(name="jurisdiction",length=40) private String jurisdiction;
  @Column(name="destination_country",length=2) private String destinationCountry;
  @Column(name="recoverable",nullable=false) private boolean recoverable;
  @Column(name="included_in_landed",nullable=false) private boolean includedInLanded=true;
  @Column(name="effective_from",nullable=false) private LocalDate effectiveFrom;
  @Column(name="effective_to") private LocalDate effectiveTo;
  @Column(name="source",length=100) private String source;
  @Column(name="version_no",nullable=false) private int versionNo=1;
  @Column(name="approval_status",length=20,nullable=false) private String approvalStatus="DRAFT";
  @Column(name="active",nullable=false) private boolean active=true;
  @Column(name="created_at",nullable=false) private Instant createdAt;
  @Column(name="updated_at",nullable=false) private Instant updatedAt;
}
