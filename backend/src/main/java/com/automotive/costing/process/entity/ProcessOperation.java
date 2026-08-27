package com.automotive.costing.process.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * A single operation within a {@code ProcessRouting} (PRC-01). Links to a machine and a
 * labor grade by code (resolved against {@code MachineMasterRepository}/
 * {@code LaborMasterRepository} at calculation time, not at save time, so a later master
 * rate change is picked up automatically on the next calculation - same pattern as
 * {@code BomNode.materialCode}).
 *
 * {@code operatorCount} is a decimal, not an int, specifically so PRC-05 ("operator:machine
 * ratios other than 1:1") is representable directly - e.g. one operator tending three
 * molding machines is {@code operatorCount = 0.333333} on each of the three machines'
 * operations, rather than needing a separate ratio field bolted on afterward.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "process_operation")
public class ProcessOperation extends BaseAuditableEntity {

    @Id
    @Column(name = "operation_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String operationId;

    @Column(name = "customer_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;

    @Column(name = "routing_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String routingId;

    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;

    /** PRC-03: which process_master entry's default cycle-time formula this operation follows. */
    @Column(name = "process_name", length = 100)
    private String processName;

    @Column(name = "machine_code", length = 50)
    private String machineCode;

    @Column(name = "labor_code", length = 50)
    private String laborCode;

    @Column(name = "cycle_time_seconds", precision = 18, scale = 6, nullable = false)
    private BigDecimal cycleTimeSeconds;

    /** PRC-05: decimal, not int - supports operator:machine ratios other than 1:1. */
    @Column(name = "operator_count", precision = 9, scale = 6, nullable = false)
    private BigDecimal operatorCount;

    /** PRC-04: nullable - not every operation has a setup/changeover cost. */
    @Column(name = "setup_time_seconds", precision = 18, scale = 6)
    private BigDecimal setupTimeSeconds;

    /** PRC-04: batch size the setup cost is amortized over. Must be > 0 if setupTimeSeconds is set. */
    @Column(name = "batch_size", precision = 18, scale = 6)
    private BigDecimal batchSize;

    /**
     * TL-03: "System shall support multi-cavity/multi-impression tooling with cost and
     * cycle-time impact reflected in both tooling and process cost." Nullable, defaulting
     * to 1 (single-cavity) - when an operation is fed by a multi-cavity tool (see
     * {@code ToolingRecord.cavityCount}), one machine cycle produces this many parts, so
     * the per-piece run cost divides by this count. Deliberately a plain field on the
     * operation (not a live FK lookup into {@code ToolingRecord}) - the engineer sets it
     * to match the tooling record's cavity count when building the routing, the same way
     * {@code cycleTimeSeconds} itself is a plain entered value rather than computed live
     * from a formula engine (see PRC-03's Javadoc in {@code ProcessRoutingDtos}).
     */
    @Column(name = "output_parts_per_cycle", precision = 9, scale = 2)
    private BigDecimal outputPartsPerCycle;

    /** V277: machine power rating for PowerCostEngine (kW). Nullable = power not configured. */
    @Column(name = "machine_kw", precision = 18, scale = 6)
    private BigDecimal machineKw;

    /** V277: electricity rate (currency unit per kWh). Nullable = use machine powerCostPerHour path if available. */
    @Column(name = "electricity_rate", precision = 18, scale = 6)
    private BigDecimal electricityRate;

    @Column(name = "electricity_rate_currency", length = 3, columnDefinition = "CHAR(3)")
    private String electricityRateCurrency;

    @Column(name = "inspection_time_hours", precision = 18, scale = 6)
    private BigDecimal inspectionTimeHours;

    @Column(name = "inspector_rate", precision = 18, scale = 6)
    private BigDecimal inspectorRate;

    @Column(name = "inspection_fixed_charge", precision = 18, scale = 6)
    private BigDecimal inspectionFixedCharge;

    @Column(name = "consumables_json", columnDefinition = "TEXT")
    private String consumablesJson;
}
