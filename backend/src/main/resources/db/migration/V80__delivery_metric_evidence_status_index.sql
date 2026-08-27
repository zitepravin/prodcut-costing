-- V80: Filter delivery metrics by evidence verification status
-- idempotent: idx_sdm_evidence_status
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'supplier_delivery_metric' AND index_name = 'idx_sdm_evidence_status'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_sdm_evidence_status ON supplier_delivery_metric (supplier_id, evidence_status, period_end DESC)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
