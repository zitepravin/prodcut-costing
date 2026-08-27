-- V57: Supplier qualification lifecycle event outbox (integration — never auto-approves)
CREATE TABLE supplier_qualification_lifecycle_event (
    event_id CHAR(36)  NOT NULL,
    qualification_id CHAR(36)  NOT NULL,
    supplier_id CHAR(36)  NOT NULL,
    event_type          VARCHAR(50)  NOT NULL,
    previous_status     VARCHAR(30),
    new_status          VARCHAR(30)  NOT NULL,
    recommendation_id CHAR(36),
    change_reason       VARCHAR(1000),
    payload_json        LONGTEXT,
    published_flag      BOOLEAN      NOT NULL DEFAULT FALSE,
    published_at        DATETIME(6),
    occurred_at         DATETIME(6)  NOT NULL,
    occurred_by         VARCHAR(36),
    created_by          VARCHAR(36),
    created_at          DATETIME(6)  NOT NULL,
    updated_by          VARCHAR(36),
    updated_at          DATETIME(6),
    PRIMARY KEY (event_id),
    INDEX idx_sqle_supplier (supplier_id, occurred_at),
    INDEX idx_sqle_unpublished (published_flag, occurred_at),
    CONSTRAINT fk_sqle_qualification FOREIGN KEY (qualification_id)
        REFERENCES supplier_qualification (qualification_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
