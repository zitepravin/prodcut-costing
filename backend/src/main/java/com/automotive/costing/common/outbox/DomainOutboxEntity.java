package com.automotive.costing.common.outbox;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Transactional Outbox row. Written in the same database transaction as the
 * business aggregate so that event publication is never lost if the process dies
 * after commit.
 *
 * Status machine: PENDING → PROCESSING → PROCESSED | FAILED → DEAD
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "domain_outbox")
public class DomainOutboxEntity {

    @Id
    @Column(name = "outbox_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String outboxId;

    @Column(name = "customer_id", length = 36, columnDefinition = "CHAR(36)")
    private String customerId;

    @Column(name = "aggregate_type", length = 80, nullable = false)
    private String aggregateType;

    @Column(name = "aggregate_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String aggregateId;

    @Column(name = "event_type", length = 120, nullable = false)
    private String eventType;

    @Column(name = "payload_json", nullable = false, columnDefinition = "JSON")
    private String payloadJson;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "status", length = 20, nullable = false)
    private String status = "PENDING";

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts = 10;

    @Column(name = "available_at", nullable = false)
    private Instant availableAt = Instant.now();

    @Column(name = "locked_by", length = 80)
    private String lockedBy;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt;
}
