package com.automotive.costing.vendorselection.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
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
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "supplier_capability")
public class SupplierCapability extends BaseAuditableEntity {

    @Id
    @Column(name = "capability_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String capabilityId;

    @Column(name = "customer_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;

    @Column(name = "supplier_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String supplierId;

    @Enumerated(EnumType.STRING)
    @Column(name = "capability_type", length = 40, nullable = false)
    private CapabilityType capabilityType;

    @Column(name = "capability_code", length = 100, nullable = false)
    private String capabilityCode;

    @Column(name = "capability_name", length = 200, nullable = false)
    private String capabilityName;

    @Column(name = "process_code", length = 100)
    private String processCode;

    @Column(name = "commodity_code", length = 100)
    private String commodityCode;

    @Column(name = "plant_code", length = 100)
    private String plantCode;

    @Column(name = "max_annual_capacity", precision = 18, scale = 4)
    private BigDecimal maxAnnualCapacity;

    @Column(name = "capacity_uom", length = 20)
    private String capacityUom;

    @Column(name = "certified", nullable = false)
    private boolean certified;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private CapabilityStatus status = CapabilityStatus.ACTIVE;

    @Column(name = "notes", length = 1000)
    private String notes;
}
