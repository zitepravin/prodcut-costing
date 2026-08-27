package com.automotive.costing.costingrun.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "costing_run")
public class CostingRun extends BaseAuditableEntity {
    @Id @Column(name = "run_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String runId;

    @Column(name = "customer_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;
    @Column(name = "run_code", length = 40, nullable = false)
    private String runCode;
    @Column(name = "description", length = 500)
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private CostingRunStatus status = CostingRunStatus.DRAFT;
    @Column(name = "program_id", length = 36, columnDefinition = "CHAR(36)")
    private String programId;
    @Column(name = "plant_code", length = 40)
    private String plantCode;
    @Column(name = "costing_variant_id", length = 36, columnDefinition = "CHAR(36)")
    private String costingVariantId;
    @Column(name = "costing_sheet_id", length = 36, columnDefinition = "CHAR(36)")
    private String costingSheetId;
    @Column(name = "lot_size", precision = 18, scale = 6)
    private BigDecimal lotSize;
    @Column(name = "costing_date")
    private LocalDate costingDate;
    @Column(name = "success_count", nullable = false)
    private int successCount;
    @Column(name = "failure_count", nullable = false)
    private int failureCount;
    @Column(name = "started_at")
    private Instant startedAt;
    @Column(name = "completed_at")
    private Instant completedAt;
}
