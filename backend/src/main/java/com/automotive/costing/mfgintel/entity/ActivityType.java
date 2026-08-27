package com.automotive.costing.mfgintel.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "activity_type")
public class ActivityType {
    @Id @Column(name = "activity_type_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String activityTypeId;
    @Column(name = "activity_code", length = 40, nullable = false)
    private String activityCode;
    @Column(name = "activity_name", length = 200, nullable = false)
    private String activityName;
    @Column(name = "category", length = 40, nullable = false)
    private String category = "MACHINE";
    @Column(name = "unit_of_measure", length = 20, nullable = false)
    private String unitOfMeasure = "HR";
    @Column(name = "default_rate", precision = 18, scale = 6)
    private BigDecimal defaultRate;
    @Column(name = "currency_code", length = 3, nullable = false)
    private String currencyCode = "USD";
    @Column(name = "cost_center_code", length = 40)
    private String costCenterCode;
    @Column(name = "active", nullable = false)
    private boolean active = true;
    @Column(name = "notes", length = 500)
    private String notes;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
