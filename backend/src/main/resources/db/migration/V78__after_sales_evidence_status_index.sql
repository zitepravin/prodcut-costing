-- V78: Filter after-sales metrics by evidence verification status
-- idempotent: idx_sas_evidence_status
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'supplier_after_sales_metric' AND index_name = 'idx_sas_evidence_status'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_sas_evidence_status ON supplier_after_sales_metric (supplier_id, evidence_status, period_end DESC)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
