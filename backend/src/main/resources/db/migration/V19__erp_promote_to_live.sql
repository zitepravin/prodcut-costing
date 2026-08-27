-- V19: ERP promote-to-live support columns (additive)
-- Promote job writes material price staging → live material_master versions.
-- Part/BOM staging acknowledgement statuses already exist in V17 enums.

ALTER TABLE erp_inbound_batch
    ADD COLUMN promoted_at DATETIME(6) NULL AFTER processed_at,
    ADD COLUMN promoted_by VARCHAR(36) NULL AFTER promoted_at;

-- idempotent: idx_erp_batch_promoted
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'erp_inbound_batch' AND index_name = 'idx_erp_batch_promoted'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_erp_batch_promoted ON erp_inbound_batch (status, promoted_at)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
