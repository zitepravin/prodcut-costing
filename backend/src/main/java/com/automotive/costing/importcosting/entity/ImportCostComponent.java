package com.automotive.costing.importcosting.entity;
import jakarta.persistence.*; import lombok.*; import java.math.BigDecimal;
@Getter @Setter @NoArgsConstructor @Entity @Table(name="import_cost_component")
public class ImportCostComponent {
  @Id @Column(name="component_id",length=36,nullable=false) private String componentId;
  @Column(name="calculation_id",length=36,nullable=false) private String calculationId;
  @Column(name="customer_id",length=36,nullable=false) private String customerId;
  @Column(name="component_code",length=40,nullable=false) private String componentCode;
  @Column(name="amount",precision=18,scale=6,nullable=false) private BigDecimal amount;
  @Column(name="currency_code",length=3,nullable=false) private String currencyCode;
  @Column(name="recoverable",nullable=false) private boolean recoverable;
  @Column(name="included_in_landed",nullable=false) private boolean includedInLanded=true;
  @Column(name="allocation_method",length=40) private String allocationMethod;
  @Column(name="allocation_pct",precision=18,scale=8) private BigDecimal allocationPct;
  @Column(name="driver_quantity",precision=18,scale=6) private BigDecimal driverQuantity;
  @Column(name="source_ref",length=100) private String sourceRef;
  @Column(name="formula_note",length=500) private String formulaNote;
  @Column(name="sort_order",nullable=false) private int sortOrder;
}
