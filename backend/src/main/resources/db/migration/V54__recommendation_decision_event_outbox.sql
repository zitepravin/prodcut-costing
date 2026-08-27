-- V54: Recommendation decision event outbox (integration / notification — never auto-approves)
CREATE TABLE vendor_recommendation_decision_event (
    event_id CHAR(36)  NOT NULL,
    recommendation_id CHAR(36)  NOT NULL,
    decision_id CHAR(36),
    explanation_id CHAR(36),
    event_type              VARCHAR(40)  NOT NULL,
    previous_status         VARCHAR(30)  NOT NULL,
    new_status              VARCHAR(30)  NOT NULL,
    recommended_supplier_id CHAR(36),
    override_supplier_id    VARCHAR(36),
    payload_json            LONGTEXT,
    published_flag          BOOLEAN      NOT NULL DEFAULT FALSE,
    published_at            DATETIME(6),
    occurred_at             DATETIME(6)  NOT NULL,
    occurred_by             VARCHAR(36),
    created_by              VARCHAR(36),
    created_at              DATETIME(6)  NOT NULL,
    updated_by              VARCHAR(36),
    updated_at              DATETIME(6),
    PRIMARY KEY (event_id),
    INDEX idx_vrde_rec (recommendation_id, occurred_at),
    INDEX idx_vrde_unpublished (published_flag, occurred_at),
    CONSTRAINT fk_vrde_recommendation FOREIGN KEY (recommendation_id)
        REFERENCES vendor_recommendation (recommendation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
