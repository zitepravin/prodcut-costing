package com.automotive.costing.common.outbox;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Claims PENDING outbox rows with FOR UPDATE SKIP LOCKED and marks them PROCESSED
 * after dispatch. Multi-instance safe at the DB claim layer.
 *
 * Current dispatch is log + status update (in-process handlers can subscribe later).
 * Failed rows retry with backoff until max_attempts → DEAD.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DomainOutboxWorker {

    private final DomainOutboxRepository repository;
    private final MeterRegistry meterRegistry;
    private final OutboxEventPublisher outboxEventPublisher;

    @Value("${app.outbox.worker-enabled:true}")
    private boolean enabled;

    @Value("${app.outbox.batch-size:20}")
    private int batchSize;

    private final String workerId = initWorkerId();

    @Scheduled(fixedDelayString = "${app.outbox.poll-ms:5000}")
    public void poll() {
        if (!enabled) {
            return;
        }
        try {
            int n = processBatch();
            if (n > 0) {
                log.debug("Outbox worker processed {} event(s)", n);
            }
        } catch (Exception ex) {
            log.error("Outbox worker poll failed: {}", ex.getMessage(), ex);
        }
    }

    @Transactional
    public int processBatch() {
        Instant now = Instant.now();
        List<DomainOutboxEntity> batch = repository.lockNextBatch(now, batchSize);
        if (batch == null || batch.isEmpty()) {
            return 0;
        }
        int done = 0;
        for (DomainOutboxEntity row : batch) {
            try {
                row.setStatus("PROCESSING");
                row.setLockedBy(workerId);
                row.setLockedAt(now);
                row.setAttemptCount(row.getAttemptCount() + 1);
                row.setUpdatedAt(now);
                repository.save(row);

                dispatch(row);

                row.setStatus("PROCESSED");
                row.setProcessedAt(Instant.now());
                row.setLastError(null);
                row.setUpdatedAt(Instant.now());
                repository.save(row);
                Counter.builder("pcs.outbox.processed").tag("result", "success")
                        .register(meterRegistry).increment();
                done++;
            } catch (Exception ex) {
                String err = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
                if (err.length() > 1000) {
                    err = err.substring(0, 1000);
                }
                row.setLastError(err);
                row.setUpdatedAt(Instant.now());
                if (row.getAttemptCount() >= row.getMaxAttempts()) {
                    row.setStatus("DEAD");
                    row.setProcessedAt(Instant.now());
                } else {
                    row.setStatus("PENDING");
                    long backoffSec = Math.min(3600L, 30L * row.getAttemptCount());
                    row.setAvailableAt(Instant.now().plusSeconds(backoffSec));
                    row.setLockedBy(null);
                    row.setLockedAt(null);
                }
                repository.save(row);
                Counter.builder("pcs.outbox.processed").tag("result", "failure")
                        .register(meterRegistry).increment();
                log.warn("Outbox event {} failed attempt {}: {}", row.getOutboxId(), row.getAttemptCount(), err);
            }
        }
        return done;
    }

    /**
     * Placeholder dispatch — replace with message broker publish or in-process event bus.
     * Must be idempotent with respect to aggregate_id + event_type where possible.
     */
    protected void dispatch(DomainOutboxEntity row) {
        outboxEventPublisher.publish(row);
    }

    private static String initWorkerId() {
        try {
            return InetAddress.getLocalHost().getHostName() + "-" + UUID.randomUUID().toString().substring(0, 8);
        } catch (Exception e) {
            return "worker-" + UUID.randomUUID().toString().substring(0, 8);
        }
    }
}
