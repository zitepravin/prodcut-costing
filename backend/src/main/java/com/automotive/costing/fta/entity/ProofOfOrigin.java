package com.automotive.costing.fta.entity;
import jakarta.persistence.*; import lombok.*; import java.time.Instant; import java.time.LocalDate;
@Getter @Setter @NoArgsConstructor @Entity @Table(name="proof_of_origin")
public class ProofOfOrigin {
  @Id @Column(name="proof_id",length=36) private String proofId;
  @Column(name="customer_id",length=36,nullable=false) private String customerId;
  @Column(name="agreement_id",length=36) private String agreementId;
  @Column(name="proof_type",length=40,nullable=false) private String proofType;
  @Column(name="reference_number",length=100) private String referenceNumber;
  @Column(name="issuer",length=200) private String issuer;
  @Column(name="producer",length=200) private String producer;
  @Column(name="origin_criterion",length=100) private String originCriterion;
  @Column(name="document_ref",length=200) private String documentRef;
  @Column(name="issue_date") private LocalDate issueDate;
  @Column(name="expiry_date") private LocalDate expiryDate;
  @Column(name="verification_status",length=20,nullable=false) private String verificationStatus="DRAFT";
  @Column(name="created_at",nullable=false) private Instant createdAt;
  @Column(name="updated_at",nullable=false) private Instant updatedAt;
}
