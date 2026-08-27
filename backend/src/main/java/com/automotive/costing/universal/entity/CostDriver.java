package com.automotive.costing.universal.entity;
import jakarta.persistence.*; import lombok.*; import java.time.Instant;
@Getter @Setter @NoArgsConstructor @Entity @Table(name="cost_driver")
public class CostDriver {
  @Id @Column(name="cost_driver_id",length=36,nullable=false,updatable=false,columnDefinition="CHAR(36)") private String costDriverId;
  @Column(name="customer_id",length=36,nullable=false,columnDefinition="CHAR(36)") private String customerId;
  @Column(name="driver_code",length=64,nullable=false) private String driverCode;
  @Column(name="driver_name",length=200,nullable=false) private String driverName;
  @Column(name="driver_type",length=40,nullable=false) private String driverType;
  @Column(name="uom",length=20) private String uom;
  @Column(name="description",length=500) private String description;
  @Column(name="active",nullable=false) private boolean active=true;
  @Column(name="version_no",nullable=false) private int versionNo=1;
  @Column(name="lifecycle_status",length=20,nullable=false) private String lifecycleStatus="ACTIVE";
  @Column(name="created_at",nullable=false) private Instant createdAt;
}
