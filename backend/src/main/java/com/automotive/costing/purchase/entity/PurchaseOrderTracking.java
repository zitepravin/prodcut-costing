package com.automotive.costing.purchase.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "purchase_order_tracking")
public class PurchaseOrderTracking extends BaseAuditableEntity {

    @Id
    @Column(name = "tracking_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String trackingId;

    @Column(name = "purchase_request_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String purchaseRequestId;

    @Column(name = "purchase_request_line_id", length = 36, columnDefinition = "CHAR(36)")
    private String purchaseRequestLineId;

    @Column(name = "external_po_reference", length = 100)
    private String externalPoReference;

    @Column(name = "tracking_status", length = 30, nullable = false)
    private String trackingStatus = "UNKNOWN";

    @Column(name = "asn_date")
    private LocalDate asnDate;

    @Column(name = "qty_shipped", precision = 18, scale = 6)
    private BigDecimal qtyShipped;

    @Column(name = "qty_ordered", precision = 18, scale = 6)
    private BigDecimal qtyOrdered;

    @Column(name = "uom", length = 20)
    private String uom;

    @Column(name = "source_system", length = 30, nullable = false)
    private String sourceSystem = "CPI";

    @Column(name = "source_event_id", length = 100)
    private String sourceEventId;

    @Column(name = "data_status", length = 20, nullable = false)
    private String dataStatus = "NOT_AVAILABLE";

    @Column(name = "human_corrected", nullable = false)
    private boolean humanCorrected;

    @Column(name = "correction_reason", length = 1000)
    private String correctionReason;

    @Column(name = "corrected_by", length = 36)
    private String correctedBy;

    @Column(name = "corrected_at")
    private Instant correctedAt;

    @Column(name = "notes", length = 1000)
    private String notes;

    /** P3-06: null = use computed; true/false = human override of on-time. */
    @Column(name = "on_time_override")
    private Boolean onTimeOverride;

    @Column(name = "on_time_override_reason", length = 1000)
    private String onTimeOverrideReason;

    @Column(name = "on_time_override_by", length = 36)
    private String onTimeOverrideBy;

    @Column(name = "on_time_override_at")
    private Instant onTimeOverrideAt;
}
