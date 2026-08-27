-- V158: Offline payment methods + idempotency for billing_payment
-- Does not duplicate V91 tables; extends billing_payment only.

ALTER TABLE billing_payment
    ADD COLUMN payment_method VARCHAR(40) NULL AFTER gateway_code,
    ADD COLUMN idempotency_key VARCHAR(64) NULL AFTER failure_reason,
    ADD COLUMN external_reference VARCHAR(120) NULL AFTER idempotency_key,
    ADD COLUMN verification_notes VARCHAR(1000) NULL AFTER external_reference,
    ADD COLUMN verified_by VARCHAR(36) NULL AFTER verification_notes,
    ADD COLUMN verified_at DATETIME(6) NULL AFTER verified_by;

CREATE UNIQUE INDEX uk_billing_payment_idempotency ON billing_payment (idempotency_key);

-- Optional: known offline method codes as documentation (no FK table required)
-- ONLINE_GATEWAY, BANK_TRANSFER, NEFT, RTGS, IMPS, UPI, CHEQUE, PURCHASE_ORDER, MANUAL, OTHER
