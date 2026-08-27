package com.automotive.costing.costingrun.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "costing_run_item")
public class CostingRunItem {
    @Id @Column(name = "item_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String itemId;
    @Column(name = "run_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String runId;
    @Column(name = "bom_node_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String bomNodeId;
    @Column(name = "part_number", length = 50)
    private String partNumber;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private CostingRunItemStatus status = CostingRunItemStatus.PENDING;
    @Column(name = "unit_cost", precision = 18, scale = 6)
    private BigDecimal unitCost;
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    @Column(name = "estimate_basis", length = 2000)
    private String estimateBasis;
    @Column(name = "calculated_at")
    private Instant calculatedAt;
}
