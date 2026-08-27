package com.automotive.costing.erp.sap.entity;

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
@Table(name = "sap_cost_estimate_staging")
public class SapCostEstimateStaging {

    @Id
    @Column(name = "staging_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String stagingId;

    @Column(name = "tenant_hint", length = 64)
    private String tenantHint;

    @Column(name = "source_system", length = 80, nullable = false)
    private String sourceSystem = "SAP";

    @Column(name = "sap_material", length = 40, nullable = false)
    private String sapMaterial;

    @Column(name = "sap_plant", length = 10, nullable = false)
    private String sapPlant;

    @Column(name = "costing_variant", length = 20)
    private String costingVariant;

    @Column(name = "costing_version", length = 20)
    private String costingVersion;

    @Column(name = "estimate_date")
    private LocalDate estimateDate;

    @Column(name = "currency_code", length = 3, columnDefinition = "CHAR(3)")
    private String currencyCode;

    @Column(name = "total_cost", precision = 18, scale = 6)
    private BigDecimal totalCost;

    @Column(name = "status", length = 20, nullable = false)
    private String status = "LANDED";

    @Column(name = "should_cost_build_up_id", length = 36, columnDefinition = "CHAR(36)")
    private String shouldCostBuildUpId;

    @Column(name = "bom_node_id", length = 36, columnDefinition = "CHAR(36)")
    private String bomNodeId;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "compared_at")
    private Instant comparedAt;

    @Column(name = "created_by", length = 36)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
