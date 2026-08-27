-- V87: Audit log filter by entity type + action (transition/verify compliance queries)
-- idempotent: idx_audit_entity_action_time
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'audit_log' AND index_name = 'idx_audit_entity_action_time'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_audit_entity_action_time ON audit_log (entity_type, action, timestamp)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
