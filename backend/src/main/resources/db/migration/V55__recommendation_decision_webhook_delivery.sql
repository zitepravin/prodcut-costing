-- V55: Decision event webhook delivery attempts (outbox consumer audit)
CREATE TABLE vendor_recommendation_event_delivery (
    delivery_id         VARCHAR(36)  NOT NULL,
    event_id CHAR(36)  NOT NULL,
    channel             VARCHAR(30)  NOT NULL,
    target_url          VARCHAR(500),
    http_status         INT,
    success_flag        BOOLEAN      NOT NULL DEFAULT FALSE,
    attempt_no          INT          NOT NULL DEFAULT 1,
    error_message       VARCHAR(2000),
    response_excerpt    VARCHAR(2000),
    attempted_at        DATETIME(6)  NOT NULL,
    created_by          VARCHAR(36),
    created_at          DATETIME(6)  NOT NULL,
    updated_by          VARCHAR(36),
    updated_at          DATETIME(6),
    PRIMARY KEY (delivery_id),
    INDEX idx_vred_event (event_id, attempted_at),
    CONSTRAINT fk_vred_event FOREIGN KEY (event_id)
        REFERENCES vendor_recommendation_decision_event (event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
