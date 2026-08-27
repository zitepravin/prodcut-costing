-- V73: Risk filter by status + severity for supplier
-- idempotent: idx_sr_supplier_status_severity
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'supplier_risk' AND index_name = 'idx_sr_supplier_status_severity'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_sr_supplier_status_severity ON supplier_risk (supplier_id, status, severity)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
