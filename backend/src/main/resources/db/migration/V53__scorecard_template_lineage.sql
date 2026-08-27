-- V53: Scorecard template lineage for configuration governance (clone keeps parent link)
ALTER TABLE vendor_scorecard_template
    ADD COLUMN parent_template_id VARCHAR(36) NULL,
    ADD COLUMN lineage_root_id VARCHAR(36) NULL,
    ADD COLUMN version_no INT NOT NULL DEFAULT 1,
    ADD COLUMN version_label VARCHAR(100) NULL;

-- idempotent: idx_vst_parent
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'vendor_scorecard_template' AND index_name = 'idx_vst_parent'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_vst_parent ON vendor_scorecard_template (parent_template_id)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
-- idempotent: idx_vst_lineage
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'vendor_scorecard_template' AND index_name = 'idx_vst_lineage'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_vst_lineage ON vendor_scorecard_template (lineage_root_id, version_no)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Seed default: self as lineage root
UPDATE vendor_scorecard_template
SET lineage_root_id = template_id, version_no = 1, version_label = 'v1-baseline'
WHERE lineage_root_id IS NULL;
