package com.automotive.costing.fta.entity;
import jakarta.persistence.*; import lombok.*; import java.math.BigDecimal; import java.time.Instant; import java.time.LocalDate;
@Getter @Setter @NoArgsConstructor @Entity @Table(name="tariff_concession")
public class TariffConcession {
  @Id @Column(name="concession_id",length=36) private String concessionId;
  @Column(name="agreement_id",length=36,nullable=false) private String agreementId;
  @Column(name="hs_code",length=32,nullable=false) private String hsCode;
  @Column(name="origin_country",length=2) private String originCountry;
  @Column(name="destination_country",length=2,nullable=false) private String destinationCountry;
  @Column(name="normal_rate",precision=18,scale=8) private BigDecimal normalRate;
  @Column(name="preferential_rate",precision=18,scale=8) private BigDecimal preferentialRate;
  @Column(name="rate_type",length=20,nullable=false) private String rateType="PERCENTAGE";
  @Column(name="specific_amount",precision=18,scale=6) private BigDecimal specificAmount;
  @Column(name="quota",precision=18,scale=6) private BigDecimal quota;
  @Column(name="exclusion_flag",nullable=false) private boolean exclusionFlag;
  @Column(name="effective_from",nullable=false) private LocalDate effectiveFrom;
  @Column(name="effective_to") private LocalDate effectiveTo;
  @Column(name="source",length=200) private String source;
  @Column(name="version_no",nullable=false) private int versionNo=1;
  @Column(name="approval_status",length=20,nullable=false) private String approvalStatus="DRAFT";
  @Column(name="active",nullable=false) private boolean active=true;
  @Column(name="created_at",nullable=false) private Instant createdAt;
  @Column(name="updated_at",nullable=false) private Instant updatedAt;
}
