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
@Table(name = "billing_payment")
public class BillingPayment extends BaseAuditableEntity {

    @Id
    @Column(name = "payment_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String paymentId;
    @Column(name = "invoice_id", nullable = false, length = 36, columnDefinition = "CHAR(36)")
    private String invoiceId;
    @Column(name = "subscription_id", nullable = false, length = 36, columnDefinition = "CHAR(36)")
    private String subscriptionId;
    @Column(name = "customer_id", nullable = false, length = 36, columnDefinition = "CHAR(36)")
    private String customerId;
    @Column(name = "gateway_code", nullable = false, length = 40)
    private String gatewayCode;
    @Column(name = "gateway_payment_ref", length = 120)
    private String gatewayPaymentRef;
    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;
    @Column(name = "status", nullable = false, length = 30)
    private String status;
    @Column(name = "initiated_at", nullable = false)
    private Instant initiatedAt;
    @Column(name = "paid_at")
    private Instant paidAt;
    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "payment_method", length = 40)
    private String paymentMethod;
    @Column(name = "idempotency_key", length = 64)
    private String idempotencyKey;
    @Column(name = "external_reference", length = 120)
    private String externalReference;
    @Column(name = "verification_notes", length = 1000)
    private String verificationNotes;
    @Column(name = "verified_by", length = 36)
    private String verifiedBy;
    @Column(name = "verified_at")
    private Instant verifiedAt;

}
