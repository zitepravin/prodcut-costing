-- V43: Supplier policy & compliance evidence register
-- Declaration is not verified evidence. WAIVED requires authorized verification + audit.
CREATE TABLE supplier_policy_evidence (
    policy_id CHAR(36)  NOT NULL,
    supplier_id CHAR(36)  NOT NULL,
    policy_type         VARCHAR(40)  NOT NULL,
    policy_title        VARCHAR(200) NOT NULL,
    description         VARCHAR(2000),
    policy_version      VARCHAR(50),
    effective_from      DATE,
    effective_to        DATE,
    expiry_date         DATE,
    declaration_only    BOOLEAN      NOT NULL DEFAULT FALSE,
    evidence_status     VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    evidence_ref        VARCHAR(255),
    verified_by CHAR(36),
    verified_at         DATETIME(6),
    waiver_reason       VARCHAR(1000),
    notes               VARCHAR(1000),
    created_by          VARCHAR(36),
    created_at          DATETIME(6)  NOT NULL,
    updated_by          VARCHAR(36),
    updated_at          DATETIME(6),
    PRIMARY KEY (policy_id),
    INDEX idx_spe_supplier_type (supplier_id, policy_type),
    INDEX idx_spe_status (evidence_status),
    INDEX idx_spe_expiry (expiry_date),
    CONSTRAINT fk_spe_supplier FOREIGN KEY (supplier_id) REFERENCES supplier_master (supplier_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
