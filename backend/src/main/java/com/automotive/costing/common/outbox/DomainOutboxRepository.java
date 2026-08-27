package com.automotive.costing.common.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface DomainOutboxRepository extends JpaRepository<DomainOutboxEntity, String> {

    /**
     * Claim a batch of pending outbox rows for a worker instance.
     * Uses status transition PENDING → PROCESSING under optimistic conditions.
     * For multi-instance production, prefer SELECT ... FOR UPDATE SKIP LOCKED
     * via a native query or dedicated claim procedure.
     */
    @Query(value = """
            SELECT * FROM domain_outbox
            WHERE status = 'PENDING'
              AND available_at <= :now
            ORDER BY created_at
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<DomainOutboxEntity> lockNextBatch(@Param("now") Instant now, @Param("batchSize") int batchSize);

    @Modifying
    @Query("""
            UPDATE DomainOutboxEntity o
               SET o.status = :status,
                   o.lockedBy = :lockedBy,
                   o.lockedAt = :lockedAt,
                   o.attemptCount = o.attemptCount + 1,
                   o.updatedAt = :lockedAt
             WHERE o.outboxId = :id
               AND o.status = 'PENDING'
            """)
    int markProcessing(@Param("id") String id,
                       @Param("status") String status,
                       @Param("lockedBy") String lockedBy,
                       @Param("lockedAt") Instant lockedAt);
}
