-- V59: Eligibility LIFECYCLE_STATUS gate — BLOCKED/SUSPENDED cannot pass (align with recommendation ranking V58)
ALTER TABLE supplier_eligibility_result
    ADD COLUMN lifecycle_status VARCHAR(30) NULL,
    ADD COLUMN lifecycle_blocked BOOLEAN NOT NULL DEFAULT FALSE;
