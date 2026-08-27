-- V29: Portal RFQ access — suppliers only see/submit against assigned OPEN RFQs
CREATE TABLE supplier_portal_rfq_access (
    access_id       VARCHAR(36)  NOT NULL,
    supplier_id CHAR(36)  NOT NULL,
    rfq_id          VARCHAR(36)  NOT NULL,
    granted_at      DATETIME(6)  NOT NULL,
    granted_by      VARCHAR(36),
    revoked_at      DATETIME(6),
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_by      VARCHAR(36),
    created_at      DATETIME(6)  NOT NULL,
    updated_by      VARCHAR(36),
    updated_at      DATETIME(6),
    PRIMARY KEY (access_id),
    UNIQUE KEY uk_portal_rfq_supplier (supplier_id, rfq_id),
    INDEX idx_portal_rfq_access_rfq (rfq_id, status),
    INDEX idx_portal_rfq_access_supplier (supplier_id, status),
    CONSTRAINT fk_portal_rfq_access_supplier FOREIGN KEY (supplier_id) REFERENCES supplier_master (supplier_id),
    CONSTRAINT fk_portal_rfq_access_rfq FOREIGN KEY (rfq_id) REFERENCES rfq_event (rfq_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
