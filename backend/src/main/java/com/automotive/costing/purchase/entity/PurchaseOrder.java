package com.automotive.costing.purchase.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "purchase_order")
public class PurchaseOrder extends BaseAuditableEntity {

    @Id
    @Column(name = "purchase_order_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String purchaseOrderId;

    @Column(name = "po_number", length = 40)
    private String poNumber;

    @Column(name = "customer_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;

    @Column(name = "purchase_request_id", length = 36, columnDefinition = "CHAR(36)")
    private String purchaseRequestId;

    @Column(name = "linked_rfq_id", length = 36, columnDefinition = "CHAR(36)")
    private String linkedRfqId;

    @Column(name = "supplier_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String supplierId;

    @Column(name = "plant_code", length = 40)
    private String plantCode;

    @Column(name = "buyer_user_id", length = 36)
    private String buyerUserId;

    @Column(name = "currency_code", length = 3, nullable = false, columnDefinition = "CHAR(3)")
    private String currencyCode = "USD";

    @Column(name = "payment_terms", length = 80)
    private String paymentTerms;

    @Column(name = "incoterms", length = 40)
    private String incoterms;

    @Column(name = "delivery_terms", length = 200)
    private String deliveryTerms;

    @Column(name = "order_date")
    private LocalDate orderDate;

    @Column(name = "required_delivery_date")
    private LocalDate requiredDeliveryDate;

    @Column(name = "status", length = 20, nullable = false)
    private String status = "DRAFT";

    @Column(name = "decision_reason", length = 1000)
    private String decisionReason;

    @Column(name = "decided_by", length = 36)
    private String decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(name = "issued_by", length = 36)
    private String issuedBy;

    @Column(name = "external_po_reference", length = 100)
    private String externalPoReference;

    @Column(name = "notes", length = 1000)
    private String notes;
}
