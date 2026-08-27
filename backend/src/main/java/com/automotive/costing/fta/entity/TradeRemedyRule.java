package com.automotive.costing.fta.entity;
import jakarta.persistence.*; import lombok.*; import java.math.BigDecimal; import java.time.Instant; import java.time.LocalDate;
@Getter @Setter @NoArgsConstructor @Entity @Table(name="trade_remedy_rule")
public class TradeRemedyRule {
  @Id @Column(name="remedy_id",length=36) private String remedyId;
  @Column(name="customer_id",length=36) private String customerId;
  @Column(name="hs_code",length=32,nullable=false) private String hsCode;
  @Column(name="origin_country",length=2,nullable=false) private String originCountry;
  @Column(name="measure_type",length=40,nullable=false) private String measureType;
  @Column(name="rate",precision=18,scale=8) private BigDecimal rate;
  @Column(name="specific_amount",precision=18,scale=6) private BigDecimal specificAmount;
  @Column(name="producer",length=200) private String producer;
  @Column(name="exporter",length=200) private String exporter;
  @Column(name="effective_from",nullable=false) private LocalDate effectiveFrom;
  @Column(name="effective_to") private LocalDate effectiveTo;
  @Column(name="source",length=200) private String source;
  @Column(name="version_no",nullable=false) private int versionNo=1;
  @Column(name="active",nullable=false) private boolean active=true;
  @Column(name="created_at",nullable=false) private Instant createdAt;
}
