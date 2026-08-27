package com.automotive.costing.importcosting.entity;
import jakarta.persistence.*; import lombok.*; import java.time.Instant; import java.time.LocalDate;
@Getter @Setter @NoArgsConstructor @Entity @Table(name="import_shipment")
public class ImportShipment {
  @Id @Column(name="shipment_id",length=36,nullable=false) private String shipmentId;
  @Column(name="customer_id",length=36,nullable=false) private String customerId;
  @Column(name="shipment_ref",length=64,nullable=false) private String shipmentRef;
  @Column(name="supplier_id",length=64) private String supplierId;
  @Column(name="supplier_country",length=2) private String supplierCountry;
  @Column(name="origin_country",length=2,nullable=false) private String originCountry;
  @Column(name="export_country",length=2,nullable=false) private String exportCountry;
  @Column(name="destination_country",length=2,nullable=false) private String destinationCountry;
  @Column(name="shipment_mode",length=20) private String shipmentMode;
  @Column(name="incoterm_code",length=10) private String incotermCode;
  @Column(name="shipment_date") private LocalDate shipmentDate;
  @Column(name="customs_date") private LocalDate customsDate;
  @Column(name="currency_code",length=3,nullable=false) private String currencyCode;
  @Column(name="status",length=20,nullable=false) private String status="DRAFT";
  @Column(name="created_at",nullable=false) private Instant createdAt;
  @Column(name="updated_at",nullable=false) private Instant updatedAt;
  @Column(name="created_by",length=64) private String createdBy;
}
