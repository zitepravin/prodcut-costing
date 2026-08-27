package com.automotive.costing.purchase.entity;

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
@Table(name = "purchase_po_link_event")
public class PurchasePoLinkEvent {
    @Id
    @Column(name = "event_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String eventId;

    @Column(name = "purchase_request_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String purchaseRequestId;

    @Column(name = "source_event_id", length = 100, nullable = false)
    private String sourceEventId;

    @Column(name = "external_po_number", length = 100, nullable = false)
    private String externalPoNumber;

    @Column(name = "external_po_system", length = 30, nullable = false)
    private String externalPoSystem = "SAP_MM";

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "received_by", length = 36)
    private String receivedBy;
}
