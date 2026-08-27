package com.automotive.costing.common.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Publishes a Spring application event so in-process listeners can react
 * without a message broker. Enable with app.outbox.publisher=spring-event.
 */
@Component
@ConditionalOnProperty(name = "app.outbox.publisher", havingValue = "spring-event")
@RequiredArgsConstructor
@Slf4j
public class SpringEventOutboxPublisher implements OutboxEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(DomainOutboxEntity event) {
        applicationEventPublisher.publishEvent(new DomainOutboxDispatchedEvent(event));
        log.debug("Outbox Spring event published id={} type={}", event.getOutboxId(), event.getEventType());
    }

    public record DomainOutboxDispatchedEvent(DomainOutboxEntity entity) {}
}
