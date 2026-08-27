package com.automotive.costing.mfgintel.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "capacity_load")
public class CapacityLoad {
    @Id @Column(name = "load_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String loadId;
    @Column(name = "capacity_center_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String capacityCenterId;
    @Column(name = "activity_type_id", length = 36, columnDefinition = "CHAR(36)")
    private String activityTypeId;
    @Column(name = "source_ref_type", length = 40)
    private String sourceRefType;
    @Column(name = "source_ref_id", length = 36, columnDefinition = "CHAR(36)")
    private String sourceRefId;
    @Column(name = "required_hours", precision = 18, scale = 6, nullable = false)
    private BigDecimal requiredHours;
    @Column(name = "load_label", length = 200)
    private String loadLabel;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
