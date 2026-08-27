package com.automotive.costing.rfq.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "portal_event_log")
public class PortalEventLog {

    @Id
    @Column(name = "event_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String eventId;

    @Column(name = "event_type", length = 40, nullable = false, updatable = false)
    private String eventType;

    @Column(name = "supplier_id", length = 36, updatable = false, columnDefinition = "CHAR(36)")
    private String supplierId;

    @Column(name = "rfq_id", length = 36, updatable = false, columnDefinition = "CHAR(36)")
    private String rfqId;

    @Column(name = "quote_id", length = 36, updatable = false, columnDefinition = "CHAR(36)")
    private String quoteId;

    @Column(name = "actor_email", length = 150, updatable = false)
    private String actorEmail;

    @Column(name = "detail", length = 1000, updatable = false)
    private String detail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
