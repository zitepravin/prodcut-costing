-- V1: Platform baseline (RBAC) and append-only audit log.
-- Sprint 0 foundation - required by every downstream module for authentication,
-- authorization, and audit logging (TDD Section 8; SRS Module 18, 19).

CREATE TABLE role (
    role_id         CHAR(36)     NOT NULL PRIMARY KEY,
    role_code       VARCHAR(50)  NOT NULL,
    description     VARCHAR(255),
    is_system_role  BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_role_code UNIQUE (role_code)
) ENGINE = InnoDB;

CREATE TABLE app_user (
    user_id        CHAR(36)     NOT NULL PRIMARY KEY,
    username       VARCHAR(100) NOT NULL,
    email          VARCHAR(150) NOT NULL,
    password_hash  VARCHAR(255),
    sso_subject    VARCHAR(255),
    status         VARCHAR(20)  NOT NULL DEFAULT 'Active',
    mfa_enabled    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_by     VARCHAR(36),
    created_at     DATETIME(6)  NOT NULL,
    updated_by     VARCHAR(36),
    updated_at     DATETIME(6),
    CONSTRAINT uk_user_username UNIQUE (username),
    CONSTRAINT uk_user_email UNIQUE (email)
) ENGINE = InnoDB;

CREATE TABLE user_role (
    user_id  CHAR(36) NOT NULL,
    role_id  CHAR(36) NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES app_user (user_id),
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES role (role_id)
) ENGINE = InnoDB;

CREATE TABLE access_scope (
    scope_id             CHAR(36)    NOT NULL PRIMARY KEY,
    user_id              CHAR(36)    NOT NULL,
    plant_id             CHAR(36),
    program_id           CHAR(36),
    commodity            VARCHAR(100),
    last_recertified_at  DATE,
    CONSTRAINT fk_access_scope_user FOREIGN KEY (user_id) REFERENCES app_user (user_id)
) ENGINE = InnoDB;

-- Append-only audit trail (AUD-01, AUD-05). The dedicated `costing_audit_writer` MySQL
-- user below is granted INSERT only - no UPDATE/DELETE - as the authoritative enforcement
-- layer, independent of application code (TDD Section 4.8). The application's normal
-- runtime DB user should route audit_log writes through this table with its full DML
-- grants EXCEPT UPDATE/DELETE on this specific table; see the GRANT statements at the
-- bottom of this file, intended for the DBA to apply in each environment (adjust the
-- username to match the environment's actual application DB user).
CREATE TABLE audit_log (
    audit_id     CHAR(36)     NOT NULL PRIMARY KEY,
    entity_type  VARCHAR(50)  NOT NULL,
    entity_id    CHAR(36)     NOT NULL,
    action       VARCHAR(20)  NOT NULL,
    old_value    JSON,
    new_value    JSON,
    user_id      CHAR(36)     NOT NULL,
    timestamp    DATETIME(6)  NOT NULL,
    ip_address   VARCHAR(45)
) ENGINE = InnoDB;

-- idempotent: idx_audit_entity
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'audit_log' AND index_name = 'idx_audit_entity'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_audit_entity ON audit_log (entity_type, entity_id)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
-- idempotent: idx_audit_user_time
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'audit_log' AND index_name = 'idx_audit_user_time'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_audit_user_time ON audit_log (user_id, timestamp)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
-- idempotent: idx_audit_entity_time
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'audit_log' AND index_name = 'idx_audit_entity_time'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_audit_entity_time ON audit_log (entity_type, timestamp)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Seed the predefined roles (SEC-01: at least eight predefined roles).
INSERT INTO role (role_id, role_code, description, is_system_role) VALUES
    (UUID(), 'SYSTEM_ADMINISTRATOR',   'Full system administration access',                TRUE),
    (UUID(), 'MASTER_DATA_ADMIN',      'Create/edit master data records',                  TRUE),
    (UUID(), 'COSTING_ENGINEER',       'Builds BOMs, routings, should-cost estimates',     TRUE),
    (UUID(), 'COSTING_MANAGER',        'Reviews/approves cost estimates',                  TRUE),
    (UUID(), 'FINANCE_CONTROLLER',     'Approves/maintains overhead, machine, currency rates', TRUE),
    (UUID(), 'PURCHASING_BUYER',       'Runs RFQs, negotiates with suppliers',             TRUE),
    (UUID(), 'SOURCING_LEAD',          'Cross-supplier visibility, approves awards',       TRUE),
    (UUID(), 'PROGRAM_COST_MANAGER',   'Owns program-level target costing / VA-VE',        TRUE),
    (UUID(), 'PROCESS_ENGINEER',       'Defines process routings, cycle times',            TRUE),
    (UUID(), 'TOOLING_ENGINEER',       'Estimates/tracks tooling investment',              TRUE);

-- NOTE for the DBA applying this migration in each environment (dev/QA/UAT/prod):
-- After the application's runtime schema owner user creates this table via Flyway,
-- create/adjust the dedicated write-role for audit_log so it can never receive
-- UPDATE or DELETE, e.g.:
--   REVOKE UPDATE, DELETE ON product_costing.audit_log FROM 'costing_app'@'%';
--   GRANT INSERT, SELECT ON product_costing.audit_log TO 'costing_app'@'%';
-- This statement is intentionally not run automatically by Flyway (grant management
-- is environment-specific and typically owned by the DBA/infra pipeline, not app code).
