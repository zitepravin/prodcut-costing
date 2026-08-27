package com.automotive.costing.universal.entity;
import jakarta.persistence.*; import lombok.*; import java.math.BigDecimal; import java.time.Instant;
@Getter @Setter @NoArgsConstructor @Entity @Table(name="cost_pool")
public class CostPool {
  @Id @Column(name="cost_pool_id",length=36,nullable=false,updatable=false,columnDefinition="CHAR(36)") private String costPoolId;
  @Column(name="customer_id",length=36,nullable=false,columnDefinition="CHAR(36)") private String customerId;
  @Column(name="pool_code",length=64,nullable=false) private String poolCode;
  @Column(name="pool_name",length=200,nullable=false) private String poolName;
  @Column(name="total_amount",precision=18,scale=6,nullable=false) private BigDecimal totalAmount=BigDecimal.ZERO;
  @Column(name="currency_code",length=3,nullable=false,columnDefinition="CHAR(3)") private String currencyCode="USD";
  @Column(name="cost_driver_id",length=36,columnDefinition="CHAR(36)") private String costDriverId;
  @Column(name="period_code",length=20) private String periodCode;
  @Column(name="active",nullable=false) private boolean active=true;
  @Column(name="version_no",nullable=false) private int versionNo=1;
  @Column(name="lifecycle_status",length=20,nullable=false) private String lifecycleStatus="DRAFT";
  @Column(name="created_at",nullable=false) private Instant createdAt;
}
