package com.automotive.costing.vendorselection.entity;
import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.Getter; import lombok.NoArgsConstructor; import lombok.Setter;
import java.math.BigDecimal; import java.time.Instant; import java.time.LocalDate;
@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "supplier_guarantee_event")
public class SupplierGuaranteeEvent extends BaseAuditableEntity {
    @Id @Column(name = "event_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)") private String eventId;
    @Column(name = "customer_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;
    @Column(name = "supplier_id", length = 36, nullable = false, columnDefinition = "CHAR(36)") private String supplierId;
    @Column(name = "guarantee_id", length = 36, columnDefinition = "CHAR(36)") private String guaranteeId;
    @Column(name = "event_date", nullable = false) private LocalDate eventDate;
    @Enumerated(EnumType.STRING) @Column(name = "event_type", length = 40, nullable = false) private GuaranteeEventType eventType;
    @Column(name = "description", length = 1000) private String description;
    @Column(name = "consequence_applied", length = 1000) private String consequenceApplied;
    @Column(name = "penalty_amount", precision = 18, scale = 6) private BigDecimal penaltyAmount;
    @Column(name = "currency_code", length = 3, columnDefinition = "CHAR(3)") private String currencyCode;
    @Column(name = "resolved_flag", nullable = false) private boolean resolvedFlag;
    @Column(name = "resolution_summary", length = 1000) private String resolutionSummary;
    @Enumerated(EnumType.STRING) @Column(name = "evidence_status", length = 20, nullable = false) private EvidenceStatus evidenceStatus = EvidenceStatus.PENDING;
    @Column(name = "evidence_ref", length = 255) private String evidenceRef;
    @Column(name = "verified_by", length = 36) private String verifiedBy;
    @Column(name = "verified_at") private Instant verifiedAt;
    @Column(name = "notes", length = 1000) private String notes;
}
