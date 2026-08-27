-- V89: Support dashboard/open-claim and feedback list patterns already used by APIs
-- open warranty claims counted by claim_status; feedback pending by evidence_status
-- idempotent: idx_swc_claim_status_only
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'supplier_warranty_claim' AND index_name = 'idx_swc_claim_status_only'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_swc_claim_status_only ON supplier_warranty_claim (claim_status)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
-- idempotent: idx_spf_evidence_only
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'supplier_performance_feedback' AND index_name = 'idx_spf_evidence_only'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_spf_evidence_only ON supplier_performance_feedback (evidence_status)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
