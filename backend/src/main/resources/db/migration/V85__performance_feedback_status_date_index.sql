-- V85: Performance feedback list by evidence status + date
-- idempotent: idx_spf_evidence_date
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'supplier_performance_feedback' AND index_name = 'idx_spf_evidence_date'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_spf_evidence_date ON supplier_performance_feedback (supplier_id, evidence_status, feedback_date DESC)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
