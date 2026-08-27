package com.automotive.costing.common.outbox;

import com.automotive.costing.common.web.CorrelationIdFilter;
import com.automotive.costing.platform.access.CustomerContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Application-facing API for writing domain events into the transactional outbox.
 *
 * Usage (inside an existing @Transactional business method):
 * <pre>
 *   outboxService.enqueue("PurchaseOrder", poId, "PURCHASE_ORDER_RELEASED", payloadMap);
 * </pre>
 *
 * The same database transaction that persists the aggregate also inserts the outbox row.
 * A separate worker later claims and dispatches the event.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DomainOutboxService {

    private final DomainOutboxRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional
    public String enqueue(String aggregateType, String aggregateId, String eventType, Map<String, Object> payload) {
        if (aggregateType == null || aggregateType.isBlank()) {
            throw new IllegalArgumentException("aggregateType is required");
        }
        if (aggregateId == null || aggregateId.isBlank()) {
            throw new IllegalArgumentException("aggregateId is required");
        }
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("eventType is required");
        }

        DomainOutboxEntity row = new DomainOutboxEntity();
        row.setOutboxId(UUID.randomUUID().toString());
        row.setCustomerId(CustomerContext.getCustomerId());
        row.setAggregateType(aggregateType.trim());
        row.setAggregateId(aggregateId.trim());
        row.setEventType(eventType.trim());
        row.setPayloadJson(toJson(payload != null ? payload : Map.of()));
        row.setCorrelationId(CorrelationIdFilter.current());
        row.setStatus("PENDING");
        row.setAttemptCount(0);
        row.setMaxAttempts(10);
        row.setAvailableAt(Instant.now());
        row.setCreatedAt(Instant.now());

        repository.save(row);
        log.debug("Outbox enqueued id={} type={} aggregate={}/{} correlation={}",
                row.getOutboxId(), eventType, aggregateType, aggregateId, row.getCorrelationId());
        return row.getOutboxId();
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to serialize outbox payload", e);
        }
    }
}
