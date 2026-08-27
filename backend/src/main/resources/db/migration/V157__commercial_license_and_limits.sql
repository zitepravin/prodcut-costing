-- V157: Commercial license, role/user limits, offline package, license history
-- Does not delete customer business data on expiry/suspension.
-- Reuses platform_customer, customer_module_entitlement, customer_feature_entitlement.

CREATE TABLE IF NOT EXISTS customer_license (
    license_id          CHAR(36)       NOT NULL,
    customer_id         CHAR(36)       NOT NULL,
    plan_code           VARCHAR(50)    NULL,
    status              VARCHAR(20)    NOT NULL,
    total_user_limit    INT            NOT NULL DEFAULT 0,
    plant_site_limit    INT            NOT NULL DEFAULT 0,
    valid_from          DATE           NOT NULL,
    valid_to            DATE           NULL,
    grace_end_date      DATE           NULL,
    payment_due_date    DATE           NULL,
    over_limit          TINYINT(1)     NOT NULL DEFAULT 0,
    offline_package_id  CHAR(36)       NULL,
    license_version     INT            NOT NULL DEFAULT 1,
    notes               VARCHAR(500)   NULL,
    created_at          DATETIME(6)    NOT NULL,
    updated_at          DATETIME(6)    NOT NULL,
    created_by          VARCHAR(36)    NULL,
    updated_by          VARCHAR(36)    NULL,
    PRIMARY KEY (license_id),
    UNIQUE KEY uk_customer_license_customer (customer_id),
    KEY idx_customer_license_status (status),
    CONSTRAINT fk_cl_customer FOREIGN KEY (customer_id) REFERENCES platform_customer (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS customer_role_limit (
    role_limit_id       CHAR(36)       NOT NULL,
    customer_id         CHAR(36)       NOT NULL,
    role_code           VARCHAR(50)    NOT NULL,
    max_users           INT            NOT NULL,
    created_at          DATETIME(6)    NOT NULL,
    updated_at          DATETIME(6)    NOT NULL,
    created_by          VARCHAR(36)    NULL,
    updated_by          VARCHAR(36)    NULL,
    PRIMARY KEY (role_limit_id),
    UNIQUE KEY uk_crl_customer_role (customer_id, role_code),
    KEY idx_crl_customer (customer_id),
    CONSTRAINT fk_crl_customer FOREIGN KEY (customer_id) REFERENCES platform_customer (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS license_history (
    history_id          CHAR(36)       NOT NULL,
    customer_id         CHAR(36)       NOT NULL,
    license_id          CHAR(36)       NULL,
    action              VARCHAR(40)    NOT NULL,
    actor_user_id       VARCHAR(36)    NULL,
    before_json         TEXT           NULL,
    after_json          TEXT           NULL,
    reason              VARCHAR(500)   NULL,
    correlation_id      VARCHAR(64)    NULL,
    created_at          DATETIME(6)    NOT NULL,
    PRIMARY KEY (history_id),
    KEY idx_lh_customer (customer_id, created_at),
    KEY idx_lh_action (action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS offline_license_package (
    package_id          CHAR(36)       NOT NULL,
    customer_id         CHAR(36)       NOT NULL,
    package_version     INT            NOT NULL,
    payload_json        MEDIUMTEXT     NOT NULL,
    signature_hex       VARCHAR(128)   NOT NULL,
    signature_alg       VARCHAR(20)    NOT NULL DEFAULT 'HmacSHA256',
    imported_at         DATETIME(6)    NOT NULL,
    imported_by         VARCHAR(36)    NULL,
    valid_from          DATE           NOT NULL,
    valid_to            DATE           NULL,
    status              VARCHAR(20)    NOT NULL,
    PRIMARY KEY (package_id),
    KEY idx_olp_customer (customer_id),
    CONSTRAINT fk_olp_customer FOREIGN KEY (customer_id) REFERENCES platform_customer (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
