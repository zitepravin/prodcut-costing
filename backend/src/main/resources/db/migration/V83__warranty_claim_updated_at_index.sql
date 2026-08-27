-- V83: Support claim status transition audit queries by updated_at
-- idempotent: idx_swc_updated_at
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'supplier_warranty_claim' AND index_name = 'idx_swc_updated_at'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_swc_updated_at ON supplier_warranty_claim (supplier_id, updated_at DESC)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
