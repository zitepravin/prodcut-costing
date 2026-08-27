package com.automotive.costing.importcosting.entity;
import jakarta.persistence.*; import lombok.*; import java.math.BigDecimal; import java.time.Instant;
@Getter @Setter @NoArgsConstructor @Entity @Table(name="import_shipment_item")
public class ImportShipmentItem {
  @Id @Column(name="item_id",length=36,nullable=false) private String itemId;
  @Column(name="shipment_id",length=36,nullable=false) private String shipmentId;
  @Column(name="customer_id",length=36,nullable=false) private String customerId;
  @Column(name="line_no",nullable=false) private int lineNo;
  @Column(name="material_id",length=64) private String materialId;
  @Column(name="product_code",length=64) private String productCode;
  @Column(name="hs_code",length=32,nullable=false) private String hsCode;
  @Column(name="quantity",precision=18,scale=6,nullable=false) private BigDecimal quantity;
  @Column(name="uom",length=20) private String uom;
  @Column(name="net_weight",precision=18,scale=6) private BigDecimal netWeight;
  @Column(name="gross_weight",precision=18,scale=6) private BigDecimal grossWeight;
  @Column(name="volume",precision=18,scale=6) private BigDecimal volume;
  @Column(name="unit_price",precision=18,scale=6,nullable=false) private BigDecimal unitPrice;
  @Column(name="invoice_currency",length=3,nullable=false) private String invoiceCurrency;
  @Column(name="invoice_value",precision=18,scale=6,nullable=false) private BigDecimal invoiceValue;
  @Column(name="created_at",nullable=false) private Instant createdAt;
}
