package com.automotive.costing.common.outbox;

/**
 * Optional external publish of claimed outbox events (message broker, webhook fan-out).
 * Default implementation is no-op logging; replace with Kafka/SQS/Rabbit when needed.
 */
public interface OutboxEventPublisher {

    void publish(DomainOutboxEntity event);
}
