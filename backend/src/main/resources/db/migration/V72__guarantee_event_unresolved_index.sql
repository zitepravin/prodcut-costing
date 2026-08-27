-- V72: KPI unresolved guarantee events (resolved_flag filter)
-- idempotent: idx_sge_unresolved
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'supplier_guarantee_event' AND index_name = 'idx_sge_unresolved'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_sge_unresolved ON supplier_guarantee_event (resolved_flag, supplier_id, event_date DESC)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
