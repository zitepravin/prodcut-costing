# Outbox publisher options

| `app.outbox.publisher` | Behavior |
|------------------------|----------|
| `logging` (default) | Log dispatch; no broker |
| `spring-event` | `ApplicationEventPublisher` → in-process listeners |

## Adding Kafka (recommended pattern)

1. Add dependency:
```xml
<dependency>
  <groupId>org.springframework.kafka</groupId>
  <artifactId>spring-kafka</artifactId>
</dependency>
```
2. Implement `OutboxEventPublisher` with `@ConditionalOnProperty(name="app.outbox.publisher", havingValue="kafka")`.
3. Message key: `{customerId}:{aggregateType}:{aggregateId}` for partition affinity per tenant aggregate.
4. Keep claim/process in DB worker (SKIP LOCKED); Kafka is downstream only.
5. Idempotent consumers required (at-least-once).

Do not dual-write from business TX to Kafka — outbox remains the source of truth.
