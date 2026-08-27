-- Controlled support access + platform owner role (no permanent business data access)

CREATE TABLE IF NOT EXISTS support_access_session (
    session_id          CHAR(36)      NOT NULL PRIMARY KEY,
    customer_id         CHAR(36)      NOT NULL,
    requested_by_user_id CHAR(36)     NOT NULL,
    approved_by_user_id CHAR(36)      NULL,
    status              VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    -- PENDING | APPROVED | DENIED | EXPIRED | REVOKED
    access_mode         VARCHAR(20)   NOT NULL DEFAULT 'READ_ONLY',
    requested_at        TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    approved_at         TIMESTAMP(6)  NULL,
    expires_at          TIMESTAMP(6)  NULL,
    duration_minutes    INT           NOT NULL DEFAULT 30,
    reason              VARCHAR(500)  NULL,
    notes               VARCHAR(1000) NULL,
    KEY idx_sas_customer (customer_id),
    KEY idx_sas_status (status),
    KEY idx_sas_req (requested_by_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- PLATFORM_OWNER role (platform control plane only)
INSERT INTO role (role_id, role_code, description, is_system_role)
SELECT UUID(), 'PLATFORM_OWNER', 'Platform owner — tenant/subscription control, no default business data', TRUE
WHERE NOT EXISTS (SELECT 1 FROM role WHERE role_code = 'PLATFORM_OWNER');

INSERT INTO role (role_id, role_code, description, is_system_role)
SELECT UUID(), 'CUSTOMER_ADMIN', 'Customer administrator — own tenant users only', TRUE
WHERE NOT EXISTS (SELECT 1 FROM role WHERE role_code = 'CUSTOMER_ADMIN');
