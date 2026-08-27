package com.automotive.costing.vendorselection.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "supplier_warranty_claim")
public class SupplierWarrantyClaim extends BaseAuditableEntity {
    @Id @Column(name = "claim_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String claimId;
    @Column(name = "customer_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;
    @Column(name = "supplier_id", length = 36, nullable = false, columnDefinition = "CHAR(36)") private String supplierId;
    @Column(name = "warranty_id", length = 36, columnDefinition = "CHAR(36)") private String warrantyId;
    @Column(name = "claim_date", nullable = false) private LocalDate claimDate;
    @Column(name = "claim_number", length = 100) private String claimNumber;
    @Column(name = "part_number", length = 100) private String partNumber;
    @Column(name = "claim_cost", precision = 18, scale = 6) private BigDecimal claimCost;
    @Column(name = "currency_code", length = 3, columnDefinition = "CHAR(3)") private String currencyCode;
    @Enumerated(EnumType.STRING) @Column(name = "claim_status", length = 20, nullable = false)
    private WarrantyClaimStatus claimStatus = WarrantyClaimStatus.OPEN;
    @Column(name = "accepted_flag") private Boolean acceptedFlag;
    @Column(name = "resolution_days") private Integer resolutionDays;
    @Column(name = "resolution_summary", length = 1000) private String resolutionSummary;
    @Enumerated(EnumType.STRING) @Column(name = "evidence_status", length = 20, nullable = false)
    private EvidenceStatus evidenceStatus = EvidenceStatus.PENDING;
    @Column(name = "evidence_ref", length = 255) private String evidenceRef;
    @Column(name = "verified_by", length = 36) private String verifiedBy;
    @Column(name = "verified_at") private Instant verifiedAt;
    @Column(name = "notes", length = 1000) private String notes;
}
