-- V77: Dashboard KPI count of ACTIVE capabilities approaching/past valid_to
-- idempotent: idx_sc_status_valid_to
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'supplier_capability' AND index_name = 'idx_sc_status_valid_to'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_sc_status_valid_to ON supplier_capability (status, valid_to)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
