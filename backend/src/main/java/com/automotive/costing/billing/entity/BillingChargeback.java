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
@Table(name = "billing_chargeback")
public class BillingChargeback extends BaseAuditableEntity {

    @Id
    @Column(name = "chargeback_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String chargebackId;
    @Column(name = "payment_id", nullable = false, length = 36, columnDefinition = "CHAR(36)")
    private String paymentId;
    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;
    @Column(name = "gateway_cb_ref", length = 120)
    private String gatewayCbRef;
    @Column(name = "status", nullable = false, length = 30)
    private String status;
    @Column(name = "review_notes", length = 1000)
    private String reviewNotes;
    @Column(name = "access_action", length = 40)
    private String accessAction;

}
