package com.automotive.costing.costingvariant.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "costing_variant")
public class CostingVariant extends BaseAuditableEntity {

    @Id
    @Column(name = "variant_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String variantId;

    @Column(name = "variant_code", length = 40, nullable = false)
    private String variantCode;

    @Column(name = "variant_name", length = 200, nullable = false)
    private String variantName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private CostingVariantStatus status = CostingVariantStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "valuation_strategy", length = 40, nullable = false)
    private ValuationStrategy valuationStrategy = ValuationStrategy.STANDARD;

    @Enumerated(EnumType.STRING)
    @Column(name = "material_price_source", length = 40, nullable = false)
    private PriceSource materialPriceSource = PriceSource.MATERIAL_MASTER;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_price_source", length = 40, nullable = false)
    private PriceSource activityPriceSource = PriceSource.MACHINE_LABOR_MASTER;

    @Enumerated(EnumType.STRING)
    @Column(name = "quantity_structure_source", length = 40, nullable = false)
    private QuantityStructureSource quantityStructureSource = QuantityStructureSource.BOM_AND_ROUTING;

    @Column(name = "currency_code", length = 3, nullable = false)
    private String currencyCode = "USD";

    @Column(name = "lot_size", precision = 18, scale = 6, nullable = false)
    private BigDecimal lotSize = BigDecimal.ONE;

    @Column(name = "scrap_pct", precision = 9, scale = 6, nullable = false)
    private BigDecimal scrapPct = BigDecimal.ZERO;

    @Column(name = "yield_pct", precision = 9, scale = 6, nullable = false)
    private BigDecimal yieldPct = BigDecimal.ONE;

    @Column(name = "overhead_pool_code", length = 40)
    private String overheadPoolCode;

    @Column(name = "cost_component_profile", length = 40)
    private String costComponentProfile;

    @Column(name = "costing_sheet_code", length = 40)
    private String costingSheetCode;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "description", length = 1000)
    private String description;
}
