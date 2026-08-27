-- V84: Risk status transition support — index for status + identified_at queries
-- idempotent: idx_sr_status_identified
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'supplier_risk' AND index_name = 'idx_sr_status_identified'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_sr_status_identified ON supplier_risk (supplier_id, status, identified_at DESC)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
