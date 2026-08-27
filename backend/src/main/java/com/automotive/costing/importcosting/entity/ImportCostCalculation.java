package com.automotive.costing.importcosting.entity;
import jakarta.persistence.*; import lombok.*; import java.math.BigDecimal; import java.time.Instant; import java.time.LocalDate;
@Getter @Setter @NoArgsConstructor @Entity @Table(name="import_cost_calculation")
public class ImportCostCalculation {
  @Id @Column(name="calculation_id",length=36,nullable=false) private String calculationId;
  @Column(name="customer_id",length=36,nullable=false) private String customerId;
  @Column(name="shipment_id",length=36) private String shipmentId;
  @Column(name="item_id",length=36) private String itemId;
  @Column(name="costing_date",nullable=false) private LocalDate costingDate;
  @Column(name="fx_as_of_date",nullable=false) private LocalDate fxAsOfDate;
  @Column(name="currency_code",length=3,nullable=false) private String currencyCode;
  @Column(name="invoice_value",precision=18,scale=6) private BigDecimal invoiceValue;
  @Column(name="customs_value",precision=18,scale=6) private BigDecimal customsValue;
  @Column(name="total_landed_cost",precision=18,scale=6) private BigDecimal totalLandedCost;
  @Column(name="unit_landed_cost",precision=18,scale=6) private BigDecimal unitLandedCost;
  @Column(name="quantity",precision=18,scale=6) private BigDecimal quantity;
  @Column(name="lifecycle_status",length=20,nullable=false) private String lifecycleStatus="DRAFT";
  @Column(name="review_status",length=40) private String reviewStatus;
  @Column(name="missing_data_json",columnDefinition="JSON") private String missingDataJson;
  @Column(name="snapshot_json",columnDefinition="JSON") private String snapshotJson;
  @Column(name="industry_profile",length=64) private String industryProfile;
  @Column(name="created_at",nullable=false) private Instant createdAt;
  @Column(name="updated_at",nullable=false) private Instant updatedAt;
  @Column(name="created_by",length=64) private String createdBy;
  @Column(name="approved_by",length=64) private String approvedBy;
  @Column(name="approved_at") private Instant approvedAt;
  @Column(name="locked_at") private Instant lockedAt;
  @Column(name="version_no",nullable=false) private int versionNo=1;
}
