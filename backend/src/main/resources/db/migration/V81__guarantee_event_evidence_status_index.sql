-- V81: Filter guarantee events by evidence verification status
-- idempotent: idx_sge_evidence_status
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'supplier_guarantee_event' AND index_name = 'idx_sge_evidence_status'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_sge_evidence_status ON supplier_guarantee_event (supplier_id, evidence_status, event_date DESC)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
