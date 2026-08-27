-- V28: External supplier portal (optional Phase 2) — accounts + one-time invites
CREATE TABLE supplier_portal_account (
    portal_account_id CHAR(36)  NOT NULL,
    supplier_id CHAR(36)  NOT NULL,
    email               VARCHAR(150) NOT NULL,
    password_hash       VARCHAR(255) NOT NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    last_login_at       DATETIME(6),
    created_by          VARCHAR(36),
    created_at          DATETIME(6)  NOT NULL,
    updated_by          VARCHAR(36),
    updated_at          DATETIME(6),
    PRIMARY KEY (portal_account_id),
    UNIQUE KEY uk_portal_email (email),
    INDEX idx_portal_supplier (supplier_id),
    CONSTRAINT fk_portal_supplier FOREIGN KEY (supplier_id) REFERENCES supplier_master (supplier_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE supplier_portal_invite (
    invite_id CHAR(36)  NOT NULL,
    supplier_id CHAR(36)  NOT NULL,
    email               VARCHAR(150) NOT NULL,
    token_hash          VARCHAR(64)  NOT NULL,
    expires_at          DATETIME(6)  NOT NULL,
    accepted_at         DATETIME(6),
    status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_by          VARCHAR(36),
    created_at          DATETIME(6)  NOT NULL,
    updated_by          VARCHAR(36),
    updated_at          DATETIME(6),
    PRIMARY KEY (invite_id),
    UNIQUE KEY uk_portal_invite_token (token_hash),
    INDEX idx_portal_invite_supplier (supplier_id, status),
    CONSTRAINT fk_portal_invite_supplier FOREIGN KEY (supplier_id) REFERENCES supplier_master (supplier_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
