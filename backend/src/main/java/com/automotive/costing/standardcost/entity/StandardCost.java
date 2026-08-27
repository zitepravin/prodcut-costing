package com.automotive.costing.standardcost.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.*;
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
@Table(name = "standard_cost")
public class StandardCost extends BaseAuditableEntity {

    @Id
    @Column(name = "standard_cost_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String standardCostId;

    @Column(name = "bom_node_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String bomNodeId;

    @Column(name = "program_id", length = 36, columnDefinition = "CHAR(36)")
    private String programId;

    @Column(name = "version_no", nullable = false)
    private int versionNo = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private StandardCostStatus status = StandardCostStatus.DRAFT;

    @Column(name = "source_type", length = 40, nullable = false)
    private String sourceType = "SHOULD_COST";

    @Column(name = "source_ref_id", length = 36, columnDefinition = "CHAR(36)")
    private String sourceRefId;

    @Column(name = "unit_cost", precision = 18, scale = 6, nullable = false)
    private BigDecimal unitCost;

    @Column(name = "currency_code", length = 3, nullable = false)
    private String currencyCode = "USD";

    @Column(name = "material_cost", precision = 18, scale = 6)
    private BigDecimal materialCost;

    @Column(name = "process_cost", precision = 18, scale = 6)
    private BigDecimal processCost;

    @Column(name = "tooling_cost", precision = 18, scale = 6)
    private BigDecimal toolingCost;

    @Column(name = "packaging_cost", precision = 18, scale = 6)
    private BigDecimal packagingCost;

    @Column(name = "overhead_cost", precision = 18, scale = 6)
    private BigDecimal overheadCost;

    @Column(name = "sga_cost", precision = 18, scale = 6)
    private BigDecimal sgaCost;

    @Column(name = "profit_margin", precision = 18, scale = 6)
    private BigDecimal profitMargin;

    @Column(name = "costing_variant_id", length = 36, columnDefinition = "CHAR(36)")
    private String costingVariantId;

    @Column(name = "plant_code", length = 40)
    private String plantCode;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "released_at")
    private Instant releasedAt;

    @Column(name = "released_by", length = 36)
    private String releasedBy;

    @Column(name = "frozen_at")
    private Instant frozenAt;

    @Column(name = "frozen_by", length = 36)
    private String frozenBy;

    @Column(name = "superseded_by_id", length = 36, columnDefinition = "CHAR(36)")
    private String supersededById;

    @Column(name = "calculation_basis", length = 2000)
    private String calculationBasis;

    @Column(name = "notes", length = 1000)
    private String notes;
}
