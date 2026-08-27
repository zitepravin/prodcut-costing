-- V60: Comparison stores lifecycle hard-stop snapshot (BLOCKED/SUSPENDED never preferred)
ALTER TABLE vendor_comparison
    ADD COLUMN lifecycle_blocked_supplier_ids VARCHAR(2000) NULL,
    ADD COLUMN lifecycle_summary VARCHAR(1000) NULL;
