package com.automotive.costing.packaging.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * M07 Packaging & Logistics profile for a BOM node.
 * Holds the cost inputs used by {@code PackagingLogisticsCostEngine}. Multiple profiles
 * per node are allowed (e.g. "Standard expendable" vs "Returnable dunnage") so engineers
 * can compare packaging strategies before locking a should-cost scenario.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "packaging_logistics_profile")
public class PackagingLogisticsProfile extends BaseAuditableEntity {

    @Id
    @Column(name = "profile_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String profileId;

    @Column(name = "customer_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;

    @Column(name = "bom_node_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String bomNodeId;

    @Column(name = "profile_label", length = 100, nullable = false)
    private String profileLabel;

    @Enumerated(EnumType.STRING)
    @Column(name = "packaging_type", length = 40, nullable = false)
    private PackagingType packagingType;

    @Column(name = "primary_pack_cost_per_piece", precision = 18, scale = 6, nullable = false)
    private BigDecimal primaryPackCostPerPiece;

    @Column(name = "secondary_pack_cost_per_piece", precision = 18, scale = 6, nullable = false)
    private BigDecimal secondaryPackCostPerPiece;

    /** Total cost of a returnable container / dunnage set; amortized over {@link #containerLifeCycles}. */
    @Column(name = "returnable_container_cost", precision = 18, scale = 6)
    private BigDecimal returnableContainerCost;

    @Column(name = "container_life_cycles", precision = 18, scale = 2)
    private BigDecimal containerLifeCycles;

    @Column(name = "net_weight_kg", precision = 18, scale = 6)
    private BigDecimal netWeightKg;

    @Enumerated(EnumType.STRING)
    @Column(name = "freight_mode", length = 20, nullable = false)
    private FreightMode freightMode;

    @Column(name = "freight_rate_per_kg", precision = 18, scale = 6)
    private BigDecimal freightRatePerKg;

    @Column(name = "fixed_freight_per_unit", precision = 18, scale = 6)
    private BigDecimal fixedFreightPerUnit;

    /** Fraction in [0, 1). Applied to (packaging + freight) as import/export duty. */
    @Column(name = "duty_pct", precision = 9, scale = 6)
    private BigDecimal dutyPct;

    @Column(name = "origin_plant_id", length = 36, columnDefinition = "CHAR(36)")
    private String originPlantId;

    @Column(name = "destination_freight_zone", length = 50)
    private String destinationFreightZone;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
