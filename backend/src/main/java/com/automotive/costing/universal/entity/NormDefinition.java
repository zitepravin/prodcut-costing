package com.automotive.costing.universal.entity;
import jakarta.persistence.*; import lombok.*;
import java.math.BigDecimal; import java.time.Instant; import java.time.LocalDate;
@Getter @Setter @NoArgsConstructor @Entity @Table(name="norm_definition")
public class NormDefinition {
  @Id @Column(name="norm_id",length=36,nullable=false,updatable=false,columnDefinition="CHAR(36)") private String normId;
  @Column(name="customer_id",length=36,nullable=false,columnDefinition="CHAR(36)") private String customerId;
  @Column(name="norm_code",length=64,nullable=false) private String normCode;
  @Column(name="description",length=500) private String description;
  @Column(name="unit",length=20) private String unit;
  @Column(name="parameter_key",length=64) private String parameterKey;
  @Column(name="formula_code",length=64) private String formulaCode;
  @Column(name="norm_value",precision=18,scale=8,nullable=false) private BigDecimal normValue;
  @Column(name="object_scope",length=40) private String objectScope;
  @Column(name="object_reference_id",length=64) private String objectReferenceId;
  @Column(name="effective_from",nullable=false) private LocalDate effectiveFrom;
  @Column(name="effective_to") private LocalDate effectiveTo;
  @Column(name="version_no",nullable=false) private int versionNo=1;
  @Column(name="active",nullable=false) private boolean active=true;
  @Column(name="approval_status",length=20,nullable=false) private String approvalStatus="DRAFT";
  @Column(name="source",length=100) private String source;
  @Column(name="created_by",length=64) private String createdBy;
  @Column(name="approved_by",length=64) private String approvedBy;
  @Column(name="approved_at") private Instant approvedAt;
  @Column(name="created_at",nullable=false) private Instant createdAt;
  @Column(name="updated_at",nullable=false) private Instant updatedAt;
}
