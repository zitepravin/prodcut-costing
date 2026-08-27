package com.automotive.costing.purchase.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "purchase_request_line")
public class PurchaseRequestLine extends BaseAuditableEntity {

    @Id
    @Column(name = "line_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String lineId;

    @Column(name = "purchase_request_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String purchaseRequestId;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    @Column(name = "material_code", length = 80)
    private String materialCode;

    @Column(name = "material_description", length = 255)
    private String materialDescription;

    @Column(name = "supplier_id", length = 36, columnDefinition = "CHAR(36)")
    private String supplierId;

    @Column(name = "supplier_quote_id", length = 36, columnDefinition = "CHAR(36)")
    private String supplierQuoteId;

    @Column(name = "quantity", precision = 18, scale = 6, nullable = false)
    private BigDecimal quantity;

    @Column(name = "uom", length = 20, nullable = false)
    private String uom = "EA";

    @Column(name = "target_price", precision = 18, scale = 6)
    private BigDecimal targetPrice;

    @Column(name = "target_currency", length = 3, columnDefinition = "CHAR(3)")
    private String targetCurrency;

    @Column(name = "cost_center_code", length = 40)
    private String costCenterCode;

    @Column(name = "unit_price", precision = 18, scale = 6)
    private BigDecimal unitPrice;

    @Column(name = "currency", length = 3, columnDefinition = "CHAR(3)")
    private String currency;

    @Column(name = "unit_price_reporting", precision = 18, scale = 6)
    private BigDecimal unitPriceReporting;

    @Column(name = "fx_rate", precision = 18, scale = 8)
    private BigDecimal fxRate;

    @Column(name = "data_status", length = 20, nullable = false)
    private String dataStatus = "NOT_AVAILABLE";

    @Column(name = "tco_unit", precision = 18, scale = 6)
    private BigDecimal tcoUnit;

    @Column(name = "tco_status", length = 20, nullable = false)
    private String tcoStatus = "NOT_AVAILABLE";

    @Column(name = "need_by_date")
    private LocalDate needByDate;

    @Column(name = "line_notes", length = 500)
    private String lineNotes;
}
