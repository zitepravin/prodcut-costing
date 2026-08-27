-- V44: Policy compliance eligibility gate support + evidence expiry run audit
-- Policy gate itself is evaluated in code against supplier_policy_evidence (V43).
-- This migration records expiry job runs for operational auditability.

CREATE TABLE evidence_expiry_run (
    run_id              VARCHAR(36)  NOT NULL,
    ran_at              DATETIME(6)  NOT NULL,
    ran_by              VARCHAR(36),
    as_of_date          DATE         NOT NULL,
    policies_expired    INT          NOT NULL DEFAULT 0,
    warranties_expired  INT          NOT NULL DEFAULT 0,
    guarantees_expired  INT          NOT NULL DEFAULT 0,
    notes               VARCHAR(1000),
    created_by          VARCHAR(36),
    created_at          DATETIME(6)  NOT NULL,
    updated_by          VARCHAR(36),
    updated_at          DATETIME(6),
    PRIMARY KEY (run_id),
    INDEX idx_eer_ran_at (ran_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Support queries for eligibility policy gate and expiry sweeps
CREATE INDEX idx_spe_supplier_status_type
    ON supplier_policy_evidence (supplier_id, evidence_status, policy_type);
