-- V82: Combined claim status + evidence filter for warranty claims list
-- idempotent: idx_swc_status_evidence
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'supplier_warranty_claim' AND index_name = 'idx_swc_status_evidence'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_swc_status_evidence ON supplier_warranty_claim (supplier_id, claim_status, evidence_status, claim_date DESC)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
