-- V31: Portal operational event log + room for extended notification_type values
ALTER TABLE app_notification
    MODIFY COLUMN notification_type VARCHAR(50) NOT NULL;

CREATE TABLE portal_event_log (
    event_id CHAR(36)  NOT NULL,
    event_type      VARCHAR(40)  NOT NULL,
    supplier_id CHAR(36),
    rfq_id          VARCHAR(36),
    quote_id        VARCHAR(36),
    actor_email     VARCHAR(150),
    detail          VARCHAR(1000),
    created_at      DATETIME(6)  NOT NULL,
    PRIMARY KEY (event_id),
    INDEX idx_portal_event_type_time (event_type, created_at),
    INDEX idx_portal_event_supplier (supplier_id, created_at),
    INDEX idx_portal_event_rfq (rfq_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
