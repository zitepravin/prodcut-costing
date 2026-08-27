package com.automotive.costing.rfq.entity;

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
@Table(name = "supplier_quote")
public class SupplierQuote extends BaseAuditableEntity {

    @Id
    @Column(name = "quote_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String quoteId;

    @Column(name = "customer_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;

    @Column(name = "rfq_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String rfqId;

    @Column(name = "rfq_line_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String rfqLineId;

    @Column(name = "supplier_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String supplierId;

    @Column(name = "bom_node_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String bomNodeId;

    @Column(name = "unit_price", precision = 18, scale = 6, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "currency_code", length = 3, nullable = false, columnDefinition = "CHAR(3)")
    private String currencyCode;

    @Column(name = "tooling_lump_sum", precision = 18, scale = 6)
    private BigDecimal toolingLumpSum;

    @Column(name = "lead_time_days")
    private Integer leadTimeDays;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private QuoteStatus status;

    @Column(name = "negotiation_reference", length = 100)
    private String negotiationReference;

    @Column(name = "notes", length = 1000)
    private String notes;
}
