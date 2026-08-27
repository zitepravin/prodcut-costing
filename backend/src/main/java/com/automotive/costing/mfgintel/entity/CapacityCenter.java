package com.automotive.costing.mfgintel.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "capacity_center")
public class CapacityCenter {
    @Id @Column(name = "capacity_center_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String capacityCenterId;
    @Column(name = "center_code", length = 40, nullable = false)
    private String centerCode;
    @Column(name = "center_name", length = 200, nullable = false)
    private String centerName;
    @Column(name = "plant_code", length = 40)
    private String plantCode;
    @Column(name = "available_hours", precision = 18, scale = 6, nullable = false)
    private BigDecimal availableHours;
    @Column(name = "period_code", length = 20, nullable = false)
    private String periodCode;
    @Column(name = "efficiency_pct", precision = 9, scale = 6, nullable = false)
    private BigDecimal efficiencyPct = BigDecimal.ONE;
    @Column(name = "active", nullable = false)
    private boolean active = true;
    @Column(name = "notes", length = 500)
    private String notes;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
