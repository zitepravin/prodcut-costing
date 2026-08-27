package com.automotive.costing.billing.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "billing_webhook_event")
public class BillingWebhookEvent extends BaseAuditableEntity {

    @Id
    @Column(name = "webhook_event_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String webhookEventId;
    @Column(name = "gateway_code", nullable = false, length = 40)
    private String gatewayCode;
    @Column(name = "gateway_event_id", nullable = false, length = 120)
    private String gatewayEventId;
    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;
    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;
    @Column(name = "signature_valid", nullable = false)
    private boolean signatureValid;
    @Column(name = "processed", nullable = false)
    private boolean processed;
    @Column(name = "processed_at")
    private Instant processedAt;
    @Column(name = "processing_result", length = 40)
    private String processingResult;
    @Column(name = "raw_payload", columnDefinition = "MEDIUMTEXT")
    private String rawPayload;
    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

}
