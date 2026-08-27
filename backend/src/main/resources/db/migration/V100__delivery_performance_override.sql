-- P3-06: human override of computed on-time classification (audit only; no SAP write)

ALTER TABLE purchase_order_tracking
    ADD COLUMN on_time_override       TINYINT(1)    NULL,
    ADD COLUMN on_time_override_reason VARCHAR(1000) NULL,
    ADD COLUMN on_time_override_by    VARCHAR(36)   NULL,
    ADD COLUMN on_time_override_at    TIMESTAMP(6)  NULL;
