package com.automotive.costing.universal.entity;
import jakarta.persistence.*; import lombok.*; import java.time.Instant;
@Getter @Setter @NoArgsConstructor @Entity @Table(name="cost_object")
public class CostObject {
  @Id @Column(name="cost_object_id",length=36,nullable=false,updatable=false,columnDefinition="CHAR(36)") private String costObjectId;
  @Column(name="customer_id",length=36,nullable=false,columnDefinition="CHAR(36)") private String customerId;
  @Column(name="object_type",length=40,nullable=false) private String objectType;
  @Column(name="object_code",length=64,nullable=false) private String objectCode;
  @Column(name="object_name",length=200,nullable=false) private String objectName;
  @Column(name="currency_code",length=3,nullable=false,columnDefinition="CHAR(3)") private String currencyCode="USD";
  @Column(name="uom",length=20) private String uom;
  @Column(name="reference_id",length=64) private String referenceId;
  @Column(name="status",length=20,nullable=false) private String status="ACTIVE";
  @Column(name="created_at",nullable=false) private Instant createdAt;
  @Column(name="updated_at",nullable=false) private Instant updatedAt;
}
