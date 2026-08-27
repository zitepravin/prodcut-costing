-- V74: Dashboard pending performance feedback count
-- idempotent: idx_spf_evidence_kpi
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'supplier_performance_feedback' AND index_name = 'idx_spf_evidence_kpi'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_spf_evidence_kpi ON supplier_performance_feedback (evidence_status, feedback_date DESC)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
