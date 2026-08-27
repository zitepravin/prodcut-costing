-- V76: List comparison snapshots by program / recent history
-- idempotent: idx_vc_program_created
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'vendor_comparison' AND index_name = 'idx_vc_program_created'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_vc_program_created ON vendor_comparison (program_id, created_at DESC)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
-- idempotent: idx_vc_created
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'vendor_comparison' AND index_name = 'idx_vc_created'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_vc_created ON vendor_comparison (created_at DESC)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
