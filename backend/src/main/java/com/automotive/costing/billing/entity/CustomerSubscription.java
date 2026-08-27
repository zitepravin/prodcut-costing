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
@Table(name = "customer_subscription")
public class CustomerSubscription extends BaseAuditableEntity {

    @Id
    @Column(name = "subscription_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String subscriptionId;
    @Column(name = "customer_id", nullable = false, length = 36, columnDefinition = "CHAR(36)")
    private String customerId;
    @Column(name = "plan_id", nullable = false, length = 36, columnDefinition = "CHAR(36)")
    private String planId;
    @Column(name = "status", nullable = false, length = 20)
    private String status;
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    @Column(name = "end_date")
    private LocalDate endDate;
    @Column(name = "billing_cycle", nullable = false, length = 20)
    private String billingCycle;
    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;
    @Column(name = "auto_renewal", nullable = false)
    private boolean autoRenewal;
    @Column(name = "trial_end_date")
    private LocalDate trialEndDate;
    @Column(name = "grace_end_date")
    private LocalDate graceEndDate;
    @Column(name = "cancelled_at")
    private Instant cancelledAt;
    @Column(name = "renewal_date")
    private LocalDate renewalDate;

}
