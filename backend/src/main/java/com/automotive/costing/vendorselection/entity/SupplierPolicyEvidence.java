package com.automotive.costing.vendorselection.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "supplier_policy_evidence")
public class SupplierPolicyEvidence extends BaseAuditableEntity {

    @Id
    @Column(name = "policy_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String policyId;
    @Column(name = "customer_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;

    @Column(name = "supplier_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String supplierId;

    @Enumerated(EnumType.STRING)
    @Column(name = "policy_type", length = 40, nullable = false)
    private PolicyType policyType;

    @Column(name = "policy_title", length = 200, nullable = false)
    private String policyTitle;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "policy_version", length = 50)
    private String policyVersion;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "declaration_only", nullable = false)
    private boolean declarationOnly = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_status", length = 20, nullable = false)
    private EvidenceStatus evidenceStatus = EvidenceStatus.PENDING;

    @Column(name = "evidence_ref", length = 255)
    private String evidenceRef;

    @Column(name = "verified_by", length = 36)
    private String verifiedBy;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "waiver_reason", length = 1000)
    private String waiverReason;

    @Column(name = "notes", length = 1000)
    private String notes;
}
