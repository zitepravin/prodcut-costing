package com.automotive.costing.vendorselection.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "vendor_recommendation_decision_event")
public class VendorRecommendationDecisionEvent extends BaseAuditableEntity {

    @Id
    @Column(name = "event_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String eventId;

    @Column(name = "recommendation_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String recommendationId;

    @Column(name = "decision_id", length = 36, columnDefinition = "CHAR(36)")
    private String decisionId;

    @Column(name = "explanation_id", length = 36, columnDefinition = "CHAR(36)")
    private String explanationId;

    @Column(name = "event_type", length = 40, nullable = false)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 30, nullable = false)
    private RecommendationStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 30, nullable = false)
    private RecommendationStatus newStatus;

    @Column(name = "recommended_supplier_id", length = 36, columnDefinition = "CHAR(36)")
    private String recommendedSupplierId;

    @Column(name = "override_supplier_id", length = 36, columnDefinition = "CHAR(36)")
    private String overrideSupplierId;

    @Lob
    @Column(name = "payload_json", columnDefinition = "LONGTEXT")
    private String payloadJson;

    @Column(name = "published_flag", nullable = false)
    private boolean publishedFlag = false;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "occurred_by", length = 36, columnDefinition = "CHAR(36)")
    private String occurredBy;
}
