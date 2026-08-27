package com.automotive.costing.common.outbox;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default publisher — records dispatch intent without requiring a broker.
 */
@Component
@ConditionalOnProperty(name = "app.outbox.publisher", havingValue = "logging", matchIfMissing = true)
@Slf4j
public class LoggingOutboxEventPublisher implements OutboxEventPublisher {

    @Override
    public void publish(DomainOutboxEntity event) {
        log.info("Outbox PUBLISH type={} aggregate={}/{} customer={} correlation={} payloadBytes={}",
                event.getEventType(),
                event.getAggregateType(),
                event.getAggregateId(),
                event.getCustomerId(),
                event.getCorrelationId(),
                event.getPayloadJson() != null ? event.getPayloadJson().length() : 0);
    }
}
