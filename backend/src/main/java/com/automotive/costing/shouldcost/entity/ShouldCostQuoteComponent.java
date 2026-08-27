package com.automotive.costing.shouldcost.entity;
import jakarta.persistence.*;
import lombok.Getter; import lombok.NoArgsConstructor; import lombok.Setter;
import java.math.BigDecimal; import java.time.Instant;
@Getter @Setter @NoArgsConstructor @Entity @Table(name="should_cost_quote_component")
public class ShouldCostQuoteComponent {
  @Id @Column(name="id", length=36, nullable=false, updatable=false, columnDefinition="CHAR(36)") private String id;
  @Column(name="build_up_id", length=36, nullable=false, columnDefinition="CHAR(36)") private String buildUpId;
  @Column(name="component_code", length=40, nullable=false) private String componentCode;
  @Column(name="quoted_amount", precision=18, scale=6, nullable=false) private BigDecimal quotedAmount;
  @Column(name="currency", length=3, nullable=false) private String currency;
  @Column(name="source_note", length=255) private String sourceNote;
  @Column(name="created_at", nullable=false) private Instant createdAt;
}
