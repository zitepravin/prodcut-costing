-- V30: SEC-02 OIDC identity links (multi-IdP ready). app_user.sso_subject remains for primary subject.
CREATE TABLE oidc_identity_link (
    link_id         VARCHAR(36)  NOT NULL,
    user_id         VARCHAR(36)  NOT NULL,
    issuer          VARCHAR(255) NOT NULL,
    subject         VARCHAR(255) NOT NULL,
    email           VARCHAR(150),
    linked_at       DATETIME(6)  NOT NULL,
    last_login_at   DATETIME(6),
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_by      VARCHAR(36),
    created_at      DATETIME(6)  NOT NULL,
    updated_by      VARCHAR(36),
    updated_at      DATETIME(6),
    PRIMARY KEY (link_id),
    UNIQUE KEY uk_oidc_issuer_subject (issuer, subject),
    INDEX idx_oidc_link_user (user_id, status),
    CONSTRAINT fk_oidc_link_user FOREIGN KEY (user_id) REFERENCES app_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Accelerate lookup when SSO subject is set on the user row
-- idempotent: idx_app_user_sso_subject
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'app_user' AND index_name = 'idx_app_user_sso_subject'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_app_user_sso_subject ON app_user (sso_subject)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
