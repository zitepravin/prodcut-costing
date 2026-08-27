-- V70: Supporting index for Supplier 360 performance feedback listing
-- idempotent: idx_spf_supplier_applied
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'supplier_performance_feedback' AND index_name = 'idx_spf_supplier_applied'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_spf_supplier_applied ON supplier_performance_feedback (supplier_id, applied_to_history, feedback_date)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
