-- V148: Tenant isolation for actual_cost_record (collation-safe)
SET @col := (SELECT COUNT(1) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'actual_cost_record' AND column_name = 'customer_id');
SET @sql := IF(@col = 0, 'ALTER TABLE actual_cost_record ADD COLUMN customer_id CHAR(36) NULL AFTER actual_cost_id', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
UPDATE actual_cost_record ac
  LEFT JOIN bom_node bn ON bn.bom_node_id COLLATE utf8mb4_unicode_ci = ac.bom_node_id COLLATE utf8mb4_unicode_ci
  SET ac.customer_id = COALESCE(bn.customer_id, '00000000-0000-0000-0000-000000000001')
  WHERE ac.customer_id IS NULL;
ALTER TABLE actual_cost_record MODIFY COLUMN customer_id CHAR(36) NOT NULL;
SET @idx := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'actual_cost_record' AND index_name = 'idx_actual_cost_customer');
SET @sql := IF(@idx = 0, 'CREATE INDEX idx_actual_cost_customer ON actual_cost_record (customer_id)', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @idx := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'actual_cost_record' AND index_name = 'idx_actual_cost_customer_bom');
SET @sql := IF(@idx = 0, 'CREATE INDEX idx_actual_cost_customer_bom ON actual_cost_record (customer_id, bom_node_id)', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
