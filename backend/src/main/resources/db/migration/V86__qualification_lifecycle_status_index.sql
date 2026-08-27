-- V86: Qualification current status lookups for lifecycle guards
-- idempotent: idx_sqc_lifecycle
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'supplier_qualification_current' AND index_name = 'idx_sqc_lifecycle'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_sqc_lifecycle ON supplier_qualification_current (lifecycle_status)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
