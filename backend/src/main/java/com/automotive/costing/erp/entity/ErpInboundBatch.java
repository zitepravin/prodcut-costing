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

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "erp_inbound_batch")
public class ErpInboundBatch {

    @Id
    @Column(name = "batch_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String batchId;

    @Column(name = "customer_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;

    @Column(name = "source_system", length = 50, nullable = false)
    private String sourceSystem;

    @Enumerated(EnumType.STRING)
    @Column(name = "batch_type", length = 30, nullable = false)
    private ErpBatchType batchType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ErpBatchStatus status = ErpBatchStatus.RECEIVED;

    @Column(name = "row_count", nullable = false)
    private int rowCount;

    @Column(name = "error_count", nullable = false)
    private int errorCount;

    @Column(name = "source_file_name", length = 255)
    private String sourceFileName;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "promoted_at")
    private Instant promotedAt;

    @Column(name = "promoted_by", length = 36, columnDefinition = "CHAR(36)")
    private String promotedBy;

    @Column(name = "created_by", length = 36, columnDefinition = "CHAR(36)")
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_by", length = 36, columnDefinition = "CHAR(36)")
    private String updatedBy;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
