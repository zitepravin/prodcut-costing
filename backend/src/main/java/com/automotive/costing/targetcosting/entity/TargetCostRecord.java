package com.automotive.costing.targetcosting.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Part-level target cost for Target Costing variance analysis.
 * Program-level target remains on {@code VehicleProgram.targetCost}; this record
 * is the BOM-node-scoped target used for should-cost / roll-up variance.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "target_cost_record")
public class TargetCostRecord extends BaseAuditableEntity {

    @Id
    @Column(name = "target_cost_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String targetCostId;

    @Column(name = "customer_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;

    @Column(name = "bom_node_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String bomNodeId;

    @Column(name = "program_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String programId;

    @Column(name = "target_amount", precision = 18, scale = 6, nullable = false)
    private BigDecimal targetAmount;

    @Column(name = "currency_code", length = 3, nullable = false, columnDefinition = "CHAR(3)")
    private String currencyCode;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private TargetCostStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", length = 20, nullable = false)
    private TargetApprovalStatus approvalStatus;

    @Column(name = "notes", length = 500)
    private String notes;
}
