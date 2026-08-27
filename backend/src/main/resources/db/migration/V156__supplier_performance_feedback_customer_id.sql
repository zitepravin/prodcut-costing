-- V156: Tenant isolation for supplier_performance_feedback
SET @col := (SELECT COUNT(1) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'supplier_performance_feedback' AND column_name = 'customer_id');
SET @sql := IF(@col = 0, 'ALTER TABLE supplier_performance_feedback ADD COLUMN customer_id CHAR(36) NULL AFTER feedback_id', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
UPDATE supplier_performance_feedback spf
  LEFT JOIN supplier_master sm ON sm.supplier_id COLLATE utf8mb4_unicode_ci = spf.supplier_id COLLATE utf8mb4_unicode_ci
  SET spf.customer_id = COALESCE(sm.customer_id, '00000000-0000-0000-0000-000000000001')
  WHERE spf.customer_id IS NULL;
ALTER TABLE supplier_performance_feedback MODIFY COLUMN customer_id CHAR(36) NOT NULL;
SET @idx := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'supplier_performance_feedback' AND index_name = 'idx_spf_customer_evidence');
SET @sql := IF(@idx = 0, 'CREATE INDEX idx_spf_customer_evidence ON supplier_performance_feedback (customer_id, evidence_status)', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
