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
@Table(name = "purchase_order_line")
public class PurchaseOrderLine extends BaseAuditableEntity {

    @Id
    @Column(name = "line_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String lineId;

    @Column(name = "purchase_order_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String purchaseOrderId;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    @Column(name = "material_code", length = 80)
    private String materialCode;

    @Column(name = "material_description", length = 255)
    private String materialDescription;

    @Column(name = "quantity", precision = 18, scale = 6, nullable = false)
    private BigDecimal quantity;

    @Column(name = "uom", length = 20, nullable = false)
    private String uom = "EA";

    @Column(name = "unit_price", precision = 18, scale = 6)
    private BigDecimal unitPrice;

    @Column(name = "currency_code", length = 3, columnDefinition = "CHAR(3)")
    private String currencyCode;

    @Column(name = "tax_amount", precision = 18, scale = 6)
    private BigDecimal taxAmount;

    @Column(name = "discount_amount", precision = 18, scale = 6)
    private BigDecimal discountAmount;

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    @Column(name = "plant_code", length = 40)
    private String plantCode;

    @Column(name = "storage_location", length = 40)
    private String storageLocation;

    @Column(name = "data_status", length = 20, nullable = false)
    private String dataStatus = "NOT_AVAILABLE";

    @Column(name = "line_notes", length = 500)
    private String lineNotes;
}
