-- V71: Link applied performance feedback to history metric rows (traceability; no auto score mutation)
ALTER TABLE supplier_performance_feedback
    ADD COLUMN history_metric_id CHAR(36) NULL AFTER applied_at,
    ADD COLUMN history_metric_kind VARCHAR(20) NULL AFTER history_metric_id;

-- idempotent: idx_spf_history_metric
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'supplier_performance_feedback' AND index_name = 'idx_spf_history_metric'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_spf_history_metric ON supplier_performance_feedback (history_metric_kind, history_metric_id)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
