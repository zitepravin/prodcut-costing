package com.automotive.costing.erp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "erp_bom_staging")
public class ErpBomStaging {

    @Id
    @Column(name = "staging_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String stagingId;

    @Column(name = "batch_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String batchId;

    @Column(name = "external_bom_id", length = 100)
    private String externalBomId;

    @Column(name = "parent_part_number", length = 50, nullable = false)
    private String parentPartNumber;

    @Column(name = "parent_revision", length = 20)
    private String parentRevision;

    @Column(name = "child_part_number", length = 50, nullable = false)
    private String childPartNumber;

    @Column(name = "child_revision", length = 20)
    private String childRevision;

    @Column(name = "quantity_per", precision = 18, scale = 6, nullable = false)
    private BigDecimal quantityPer;

    @Column(name = "uom", length = 20)
    private String uom;

    @Column(name = "line_number")
    private Integer lineNumber;

    @Column(name = "plant_code", length = 50)
    private String plantCode;

    @Column(name = "program_code", length = 50)
    private String programCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ErpStagingRowStatus status = ErpStagingRowStatus.PENDING;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
