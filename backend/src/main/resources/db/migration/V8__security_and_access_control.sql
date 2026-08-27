-- M18 User Management, Roles & Security (SRS 4.18, TDD 5.18/8, Test Plan 7.18/9.2). SEC-01..SEC-08.

-- SEC-07: TOTP MFA secret (set on enrollment, only takes effect once mfa_enabled is
-- flipped true by a confirmed enrollment - see MfaService). SEC-08: token_version is the
-- JWT-revocation mechanism - incrementing it invalidates every previously-issued token for
-- this user without needing a separate token blocklist store (see JwtTokenProvider's
-- Javadoc). SEC-06: password_updated_at supports a future expiry-policy check;
-- failed_login_attempts is a lightweight brute-force signal (not itself a lockout
-- mechanism yet - see NEXT_AGENT.md for the account-lockout follow-up this enables but
-- doesn't yet implement).
ALTER TABLE app_user
    ADD COLUMN token_version         INT          NOT NULL DEFAULT 0,
    ADD COLUMN mfa_secret            VARCHAR(64),
    ADD COLUMN password_updated_at   DATETIME(6),
    ADD COLUMN last_login_at         DATETIME(6),
    ADD COLUMN failed_login_attempts INT          NOT NULL DEFAULT 0;

-- SEC-03: "System shall enforce data segregation by plant, program, and commodity as
-- configured by an administrator." An empty scope set for a user (no rows here) means
-- unrestricted access - the same "absence = no restriction, not no access" convention
-- used for nullable fields elsewhere in this system, chosen deliberately so that seeding
-- the first administrator account doesn't require also seeding scope rows just to let
-- them see anything.
CREATE TABLE user_access_scope (
    scope_id     CHAR(36)     NOT NULL PRIMARY KEY,
    user_id       CHAR(36)     NOT NULL,
    scope_type      VARCHAR(20)  NOT NULL, -- PLANT / PROGRAM / COMMODITY
    scope_value        VARCHAR(100) NOT NULL,
    created_by            VARCHAR(36),
    created_at              DATETIME(6)  NOT NULL,
    CONSTRAINT fk_user_access_scope_user FOREIGN KEY (user_id) REFERENCES app_user (user_id)
) ENGINE = InnoDB;

-- idempotent: idx_user_access_scope_user
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'user_access_scope' AND index_name = 'idx_user_access_scope_user'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_user_access_scope_user ON user_access_scope (user_id, scope_type)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- SEC-04: confidentiality wall configuration per RFQ event (SRS 4.18.3/4.18.4). Config
-- data model only in this session - M09 (RFQ Management, the source of a real RFQ event
-- and the supplier_quote table this is meant to restrict access to) is Phase 2 and
-- doesn't exist yet. See ConfidentialityWall's Javadoc for the forward-reference pattern.
CREATE TABLE confidentiality_wall (
    wall_id           CHAR(36)     NOT NULL PRIMARY KEY,
    rfq_event_reference VARCHAR(100) NOT NULL, -- forward reference to a future rfq.rfq_id
    allowed_role_code      VARCHAR(50),
    allowed_user_id           CHAR(36),
    created_by                  VARCHAR(36),
    created_at                    DATETIME(6)  NOT NULL,
    CONSTRAINT fk_confidentiality_wall_user FOREIGN KEY (allowed_user_id) REFERENCES app_user (user_id)
) ENGINE = InnoDB;

-- idempotent: idx_confidentiality_wall_rfq
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'confidentiality_wall' AND index_name = 'idx_confidentiality_wall_rfq'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_confidentiality_wall_rfq ON confidentiality_wall (rfq_event_reference)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
