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
@Table(name = "billing_invoice")
public class BillingInvoice extends BaseAuditableEntity {

    @Id
    @Column(name = "invoice_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String invoiceId;
    @Column(name = "invoice_number", nullable = false, length = 40)
    private String invoiceNumber;
    @Column(name = "customer_id", nullable = false, length = 36, columnDefinition = "CHAR(36)")
    private String customerId;
    @Column(name = "subscription_id", nullable = false, length = 36, columnDefinition = "CHAR(36)")
    private String subscriptionId;
    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;
    @Column(name = "subtotal", nullable = false, precision = 19, scale = 4)
    private BigDecimal subtotal;
    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal taxAmount;
    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal discountAmount;
    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount;
    @Column(name = "payment_status", nullable = false, length = 20)
    private String paymentStatus;
    @Column(name = "payment_reference", length = 100)
    private String paymentReference;
    @Column(name = "pdf_reference", length = 500)
    private String pdfReference;

}
