package com.automotive.costing.fta.entity;
import jakarta.persistence.*; import lombok.*; import java.time.Instant; import java.time.LocalDate;
@Getter @Setter @NoArgsConstructor @Entity @Table(name="trade_agreement")
public class TradeAgreement {
  @Id @Column(name="agreement_id",length=36) private String agreementId;
  @Column(name="agreement_code",length=40,nullable=false) private String agreementCode;
  @Column(name="agreement_name",length=200,nullable=false) private String agreementName;
  @Column(name="agreement_type",length=40) private String agreementType;
  @Column(name="region",length=40) private String region;
  @Column(name="status",length=20,nullable=false) private String status="DRAFT";
  @Column(name="signed_date") private LocalDate signedDate;
  @Column(name="effective_date") private LocalDate effectiveDate;
  @Column(name="expiry_date") private LocalDate expiryDate;
  @Column(name="review_date") private LocalDate reviewDate;
  @Column(name="version_no",nullable=false) private int versionNo=1;
  @Column(name="source",length=200) private String source;
  @Column(name="official_reference",length=500) private String officialReference;
  @Column(name="enabled",nullable=false) private boolean enabled=true;
  @Column(name="platform_scope",nullable=false) private boolean platformScope=true;
  @Column(name="last_verified") private LocalDate lastVerified;
  @Column(name="approval_status",length=20,nullable=false) private String approvalStatus="DRAFT";
  @Column(name="created_at",nullable=false) private Instant createdAt;
  @Column(name="updated_at",nullable=false) private Instant updatedAt;
}
