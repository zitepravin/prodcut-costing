package com.automotive.costing.vendorselection.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "supplier_tco_calculation")
public class SupplierTcoCalculation extends BaseAuditableEntity {

    @Id
    @Column(name = "tco_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String tcoId;

    @Column(name = "customer_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;

    @Column(name = "supplier_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String supplierId;

    @Column(name = "bom_node_id", length = 36, columnDefinition = "CHAR(36)")
    private String bomNodeId;

    @Column(name = "program_id", length = 36, columnDefinition = "CHAR(36)")
    private String programId;

    @Column(name = "currency_code", length = 3, nullable = false, columnDefinition = "CHAR(3)")
    private String currencyCode;

    @Column(name = "annual_volume", precision = 18, scale = 6)
    private BigDecimal annualVolume;

    @Column(name = "purchase_price", precision = 18, scale = 6)
    private BigDecimal purchasePrice;

    @Column(name = "freight", precision = 18, scale = 6)
    private BigDecimal freight;

    @Column(name = "logistics", precision = 18, scale = 6)
    private BigDecimal logistics;

    @Column(name = "packaging", precision = 18, scale = 6)
    private BigDecimal packaging;

    @Column(name = "inventory_cost", precision = 18, scale = 6)
    private BigDecimal inventoryCost;

    @Column(name = "quality_rejection_cost", precision = 18, scale = 6)
    private BigDecimal qualityRejectionCost;

    @Column(name = "rework_cost", precision = 18, scale = 6)
    private BigDecimal reworkCost;

    @Column(name = "warranty_cost", precision = 18, scale = 6)
    private BigDecimal warrantyCost;

    @Column(name = "service_cost", precision = 18, scale = 6)
    private BigDecimal serviceCost;

    @Column(name = "tooling_cost", precision = 18, scale = 6)
    private BigDecimal toolingCost;

    @Column(name = "other_cost", precision = 18, scale = 6)
    private BigDecimal otherCost;

    @Column(name = "tco_total", precision = 18, scale = 6)
    private BigDecimal tcoTotal;

    @Column(name = "missing_components", length = 500)
    private String missingComponents;

    @Column(name = "data_complete", nullable = false)
    private boolean dataComplete;

    @Lob
    @Column(name = "calculation_json", columnDefinition = "LONGTEXT")
    private String calculationJson;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;

    @Column(name = "calculated_by", length = 36, columnDefinition = "CHAR(36)")
    private String calculatedBy;
}
