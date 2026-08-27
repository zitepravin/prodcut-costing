-- V51: Questionnaire response evidence verification (verify/fail/waive — parallel to policy evidence)
ALTER TABLE vendor_questionnaire_response
    ADD COLUMN verified_by CHAR(36) NULL,
    ADD COLUMN verified_at DATETIME(6) NULL,
    ADD COLUMN waiver_reason VARCHAR(1000) NULL;

CREATE INDEX idx_vqr_evidence_status
    ON vendor_questionnaire_response (evidence_status, supplier_id);
