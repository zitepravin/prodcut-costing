-- V136: process_master + process_routing + process_operation tenant ownership
-- Classification: process definitions and routings are tenant-owned.
SET @default_customer_id := (
    SELECT customer_id FROM platform_customer WHERE customer_code = 'DEFAULT' LIMIT 1
);
SET @default_customer_id := IFNULL(@default_customer_id, '00000000-0000-0000-0000-000000000001');

SET @tbl := (SELECT COUNT(1) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'process_master');
SET @col := (SELECT COUNT(1) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'process_master' AND column_name = 'customer_id');
SET @sql := IF(@tbl > 0 AND @col = 0, 'ALTER TABLE process_master ADD COLUMN customer_id CHAR(36) NULL', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
UPDATE process_master SET customer_id = @default_customer_id WHERE customer_id IS NULL;
SET @sql := IF(@tbl > 0, 'ALTER TABLE process_master MODIFY COLUMN customer_id CHAR(36) NOT NULL', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @idx := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'process_master' AND index_name = 'idx_pm_customer');
SET @sql := IF(@tbl > 0 AND @idx = 0, 'CREATE INDEX idx_pm_customer ON process_master (customer_id)', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @tbl := (SELECT COUNT(1) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'process_routing');
SET @col := (SELECT COUNT(1) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'process_routing' AND column_name = 'customer_id');
SET @sql := IF(@tbl > 0 AND @col = 0, 'ALTER TABLE process_routing ADD COLUMN customer_id CHAR(36) NULL', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
UPDATE process_routing SET customer_id = @default_customer_id WHERE customer_id IS NULL;
SET @sql := IF(@tbl > 0, 'ALTER TABLE process_routing MODIFY COLUMN customer_id CHAR(36) NOT NULL', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @idx := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'process_routing' AND index_name = 'idx_pr_customer');
SET @sql := IF(@tbl > 0 AND @idx = 0, 'CREATE INDEX idx_pr_customer ON process_routing (customer_id)', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @tbl := (SELECT COUNT(1) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'process_operation');
SET @col := (SELECT COUNT(1) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'process_operation' AND column_name = 'customer_id');
SET @sql := IF(@tbl > 0 AND @col = 0, 'ALTER TABLE process_operation ADD COLUMN customer_id CHAR(36) NULL', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
UPDATE process_operation SET customer_id = @default_customer_id WHERE customer_id IS NULL;
SET @sql := IF(@tbl > 0, 'ALTER TABLE process_operation MODIFY COLUMN customer_id CHAR(36) NOT NULL', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
