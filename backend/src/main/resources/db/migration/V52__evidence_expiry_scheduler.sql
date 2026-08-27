-- V52: Scheduled evidence expiry job support (trigger source + last schedule status)
ALTER TABLE evidence_expiry_run
    ADD COLUMN trigger_source VARCHAR(20) NOT NULL DEFAULT 'MANUAL';

CREATE TABLE evidence_expiry_job_status (
    status_id           VARCHAR(36)  NOT NULL,
    job_name            VARCHAR(100) NOT NULL,
    enabled_flag        BOOLEAN      NOT NULL DEFAULT TRUE,
    last_scheduled_at   DATETIME(6),
    last_run_id         VARCHAR(36),
    last_success        BOOLEAN,
    last_error          VARCHAR(2000),
    updated_at          DATETIME(6)  NOT NULL,
    PRIMARY KEY (status_id),
    UNIQUE KEY uk_eejs_job_name (job_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO evidence_expiry_job_status (status_id, job_name, enabled_flag, updated_at)
SELECT UUID(), 'EVIDENCE_EXPIRY_DAILY', TRUE, UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM evidence_expiry_job_status WHERE job_name = 'EVIDENCE_EXPIRY_DAILY');
