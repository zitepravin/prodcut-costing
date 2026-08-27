-- V75: Filter warranty claims by evidence verification status
-- idempotent: idx_swc_evidence_status
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'supplier_warranty_claim' AND index_name = 'idx_swc_evidence_status'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_swc_evidence_status ON supplier_warranty_claim (supplier_id, evidence_status, claim_date DESC)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
