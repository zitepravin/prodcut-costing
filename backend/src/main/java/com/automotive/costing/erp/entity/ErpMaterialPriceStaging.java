package com.automotive.costing.erp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "erp_material_price_staging")
public class ErpMaterialPriceStaging {

    @Id
    @Column(name = "staging_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String stagingId;

    @Column(name = "batch_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String batchId;

    @Column(name = "material_code", length = 50, nullable = false)
    private String materialCode;

    @Column(name = "plant_code", length = 50)
    private String plantCode;

    @Column(name = "unit_price", precision = 18, scale = 6, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "currency_code", length = 3, nullable = false, columnDefinition = "CHAR(3)")
    private String currencyCode;

    @Column(name = "uom", length = 20)
    private String uom;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "supplier_code", length = 50)
    private String supplierCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ErpStagingRowStatus status = ErpStagingRowStatus.PENDING;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
