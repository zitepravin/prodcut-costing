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
@Table(name = "supplier_guarantee_term")
public class SupplierGuaranteeTerm extends BaseAuditableEntity {
    @Id
    @Column(name = "guarantee_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String guaranteeId;
    @Column(name = "customer_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;
    @Column(name = "supplier_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String supplierId;
    @Enumerated(EnumType.STRING)
    @Column(name = "guarantee_type", length = 40, nullable = false)
    private GuaranteeType guaranteeType;
    @Column(name = "description", length = 1000)
    private String description;
    @Column(name = "period_months")
    private Integer periodMonths;
    @Column(name = "conditions_text", length = 1000)
    private String conditionsText;
    @Column(name = "failure_consequence", length = 1000)
    private String failureConsequence;
    @Column(name = "effective_from")
    private LocalDate effectiveFrom;
    @Column(name = "effective_to")
    private LocalDate effectiveTo;
    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_status", length = 20, nullable = false)
    private EvidenceStatus evidenceStatus = EvidenceStatus.PENDING;
    @Column(name = "evidence_ref", length = 255)
    private String evidenceRef;
    @Column(name = "verified_by", length = 36)
    private String verifiedBy;
    @Column(name = "verified_at")
    private Instant verifiedAt;
    @Column(name = "notes", length = 1000)
    private String notes;
}
