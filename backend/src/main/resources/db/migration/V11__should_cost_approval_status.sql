-- Closes a gap flagged in the M14 session's own CHANGELOG.md entry: WF-06/SRS 4.14.5
-- requires "a rejected cost record returns to 'Draft' status", but should_cost_build_up
-- had no Draft-equivalent status to revert to. Adding a column here (V11) rather than
-- editing the already-shipped V7__should_cost_module.sql, per Flyway's core rule that a
-- checksummed, already-applied migration is never edited after the fact.
ALTER TABLE should_cost_build_up
    ADD COLUMN approval_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT';

-- idempotent: idx_should_cost_build_up_approval_status
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'should_cost_build_up' AND index_name = 'idx_should_cost_build_up_approval_status'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_should_cost_build_up_approval_status ON should_cost_build_up (approval_status)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
