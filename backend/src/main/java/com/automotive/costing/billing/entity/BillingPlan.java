package com.automotive.costing.billing.entity;

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
@Table(name = "billing_plan")
public class BillingPlan extends BaseAuditableEntity {

    @Id
    @Column(name = "plan_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String planId;
    @Column(name = "plan_code", nullable = false, length = 50)
    private String planCode;
    @Column(name = "plan_name", nullable = false, length = 150)
    private String planName;
    @Column(name = "description", length = 500)
    private String description;
    @Column(name = "billing_frequency", nullable = false, length = 20)
    private String billingFrequency;
    @Column(name = "price", nullable = false, precision = 19, scale = 4)
    private BigDecimal price;
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;
    @Column(name = "trial_days", nullable = false)
    private int trialDays;
    @Column(name = "grace_days", nullable = false)
    private int graceDays;
    @Column(name = "status", nullable = false, length = 20)
    private String status;

}
