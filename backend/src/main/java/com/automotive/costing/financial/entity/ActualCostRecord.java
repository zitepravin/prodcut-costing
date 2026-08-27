package com.automotive.costing.financial.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "actual_cost_record")
public class ActualCostRecord {

    @Id
    @Column(name = "actual_cost_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String actualCostId;

    @Column(name = "customer_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;

    @Column(name = "bom_node_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String bomNodeId;

    @Column(name = "program_id", length = 36, columnDefinition = "CHAR(36)")
    private String programId;

    @Column(name = "source_system", length = 40, nullable = false)
    private String sourceSystem = "IMPORT";

    @Column(name = "source_ref", length = 100)
    private String sourceRef;

    @Column(name = "period_code", length = 20)
    private String periodCode;

    @Column(name = "quantity", precision = 18, scale = 6)
    private BigDecimal quantity;

    @Column(name = "unit_cost", precision = 18, scale = 6, nullable = false)
    private BigDecimal unitCost;

    @Column(name = "total_cost", precision = 18, scale = 6)
    private BigDecimal totalCost;

    @Column(name = "currency_code", length = 3, nullable = false)
    private String currencyCode = "USD";

    @Column(name = "material_cost", precision = 18, scale = 6)
    private BigDecimal materialCost;

    @Column(name = "process_cost", precision = 18, scale = 6)
    private BigDecimal processCost;

    @Column(name = "overhead_cost", precision = 18, scale = 6)
    private BigDecimal overheadCost;

    @Column(name = "other_cost", precision = 18, scale = 6)
    private BigDecimal otherCost;

    @Column(name = "posting_date")
    private LocalDate postingDate;

    @Column(name = "imported_at", nullable = false)
    private Instant importedAt = Instant.now();

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_by", length = 36)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
