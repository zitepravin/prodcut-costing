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
@Table(name = "vendor_recommendation_event_delivery")
public class VendorRecommendationEventDelivery extends BaseAuditableEntity {

    @Id
    @Column(name = "delivery_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String deliveryId;

    @Column(name = "event_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String eventId;

    @Column(name = "channel", length = 30, nullable = false)
    private String channel;

    @Column(name = "target_url", length = 500)
    private String targetUrl;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "success_flag", nullable = false)
    private boolean successFlag;

    @Column(name = "attempt_no", nullable = false)
    private int attemptNo = 1;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "response_excerpt", length = 2000)
    private String responseExcerpt;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;
}
