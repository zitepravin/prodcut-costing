package com.automotive.costing.universal.entity;
import jakarta.persistence.*; import lombok.*;
import java.math.BigDecimal; import java.time.Instant; import java.time.LocalDate;
@Getter @Setter @NoArgsConstructor @Entity @Table(name="supplier_share_of_business")
public class SupplierShareOfBusiness {
  @Id @Column(name="sob_id",length=36,nullable=false,updatable=false,columnDefinition="CHAR(36)") private String sobId;
  @Column(name="customer_id",length=36,nullable=false,columnDefinition="CHAR(36)") private String customerId;
  @Column(name="cost_object_id",length=36,columnDefinition="CHAR(36)") private String costObjectId;
  @Column(name="product_code",length=64) private String productCode;
  @Column(name="supplier_id",length=36,nullable=false,columnDefinition="CHAR(36)") private String supplierId;
  @Column(name="allocation_percent",precision=9,scale=4,nullable=false) private BigDecimal allocationPercent;
  @Column(name="allocation_basis",length=40,nullable=false) private String allocationBasis="VOLUME";
  @Column(name="allocation_group_code",length=64,nullable=false) private String allocationGroupCode;
  @Column(name="effective_from",nullable=false) private LocalDate effectiveFrom;
  @Column(name="effective_to") private LocalDate effectiveTo;
  @Column(name="status",length=20,nullable=false) private String status="ACTIVE";
  @Column(name="priority",nullable=false) private int priority=1;
  @Column(name="version_no",nullable=false) private int versionNo=1;
  @Column(name="created_by",length=64) private String createdBy;
  @Column(name="created_at",nullable=false) private Instant createdAt;
  @Column(name="updated_at",nullable=false) private Instant updatedAt;
}
