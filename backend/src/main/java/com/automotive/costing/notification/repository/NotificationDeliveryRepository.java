package com.automotive.costing.notification.repository;

import com.automotive.costing.notification.entity.NotificationDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, String> {

    Optional<NotificationDelivery> findByIdempotencyKey(String idempotencyKey);

    /** @deprecated Prefer {@link #lockDueBatch} for multi-instance workers */
    @Deprecated
    List<NotificationDelivery> findTop50ByStatusInAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
            List<String> statuses, Instant now);

    /**
     * Multi-instance safe claim using FOR UPDATE SKIP LOCKED.
     * Must be called inside a transaction.
     */
    @Query(value = """
            SELECT * FROM notification_delivery d
            WHERE d.status IN ('QUEUED', 'RETRYING')
              AND d.scheduled_at <= :now
            ORDER BY d.scheduled_at ASC
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<NotificationDelivery> lockDueBatch(@Param("now") Instant now, @Param("batchSize") int batchSize);
}
