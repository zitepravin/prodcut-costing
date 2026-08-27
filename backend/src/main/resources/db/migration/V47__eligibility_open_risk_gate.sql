-- V47: Open risk eligibility gate — risk remains visible and can block eligibility
ALTER TABLE supplier_eligibility_result
    ADD COLUMN require_no_critical_open_risks BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN require_no_high_open_risks BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN open_critical_risk_count INT NULL,
    ADD COLUMN open_high_risk_count INT NULL,
    ADD COLUMN open_risk_count INT NULL;
