-- V88: Combined claimStatus + evidenceStatus list filter (AND)
-- Note: V82 has (supplier_id, claim_status, evidence_status, claim_date); this documents intent.
-- Add covering index only if distinct name needed for unresolved event combo queries.
-- idempotent: idx_sge_resolved_evidence
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'supplier_guarantee_event' AND index_name = 'idx_sge_resolved_evidence'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_sge_resolved_evidence ON supplier_guarantee_event (supplier_id, resolved_flag, evidence_status, event_date DESC)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
