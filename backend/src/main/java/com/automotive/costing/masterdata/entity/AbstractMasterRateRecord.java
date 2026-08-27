package com.automotive.costing.masterdata.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Shared columns for material_master / machine_master / labor_master / overhead_pool
 * (DATABASE_SCHEMA.md Section 2 - "structurally similar" family).
 *
 * MDM-01: code is unique and non-duplicable per concrete table (enforced by a DB
 * unique constraint on (code, version_number is NOT part of uniqueness - code alone
 * is unique per record LINEAGE; new versions reuse the same code with a new row and
 * incremented version_number, distinguished from the original by record_id).
 * MDM-02: version_number + effective_from/effective_to together implement versioning.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class AbstractMasterRateRecord extends BaseAuditableEntity {

    @Id
    @Column(name = "record_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String recordId;

    /** Tenant owner — material/machine/labor are tenant catalogs per TABLE_TENANT_CLASSIFICATION. */
    @Column(name = "customer_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;

    @Column(name = "code", length = 50, nullable = false)
    private String code;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "rate", precision = 18, scale = 6, nullable = false)
    private BigDecimal rate;

    @Column(name = "uom", length = 20)
    private String uom;

    @Column(name = "plant_id", length = 36, columnDefinition = "CHAR(36)")
    private String plantId; // null = Global, per SRS 4.1.5 "belong to exactly one plant or Global"

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private MasterDataStatus status;
}
