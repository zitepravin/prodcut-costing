-- V63: Capability expiry pass — ACTIVE capabilities past valid_to become EXPIRED
CREATE TABLE capability_expiry_run (
    run_id              VARCHAR(36)  NOT NULL,
    ran_at              DATETIME(6)  NOT NULL,
    ran_by              VARCHAR(36),
    as_of_date          DATE         NOT NULL,
    capabilities_expired INT         NOT NULL DEFAULT 0,
    trigger_source      VARCHAR(20)  NOT NULL DEFAULT 'MANUAL',
    notes               VARCHAR(1000),
    created_by          VARCHAR(36),
    created_at          DATETIME(6)  NOT NULL,
    updated_by          VARCHAR(36),
    updated_at          DATETIME(6),
    PRIMARY KEY (run_id),
    INDEX idx_cer_ran_at (ran_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE capability_expiry_job_status (
    status_id           VARCHAR(36)  NOT NULL,
    job_name            VARCHAR(100) NOT NULL,
    enabled_flag        BOOLEAN      NOT NULL DEFAULT TRUE,
    last_scheduled_at   DATETIME(6),
    last_run_id         VARCHAR(36),
    last_success        BOOLEAN,
    last_error          VARCHAR(2000),
    updated_at          DATETIME(6)  NOT NULL,
    PRIMARY KEY (status_id),
    UNIQUE KEY uk_cejs_job_name (job_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO capability_expiry_job_status (status_id, job_name, enabled_flag, updated_at)
SELECT UUID(), 'CAPABILITY_EXPIRY_DAILY', TRUE, UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM capability_expiry_job_status WHERE job_name = 'CAPABILITY_EXPIRY_DAILY');

-- idempotent: idx_sc_status_valid_to
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'supplier_capability' AND index_name = 'idx_sc_status_valid_to'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_sc_status_valid_to ON supplier_capability (status, valid_to)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
