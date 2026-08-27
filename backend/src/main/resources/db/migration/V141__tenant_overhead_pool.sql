-- V141: overhead_pool tenant ownership (same family as material/machine/labor)
SET @default_customer_id := (
    SELECT customer_id FROM platform_customer WHERE customer_code = 'DEFAULT' LIMIT 1
);
SET @default_customer_id := IFNULL(@default_customer_id, '00000000-0000-0000-0000-000000000001');

SET @tbl := (SELECT COUNT(1) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'overhead_pool');
SET @col := (SELECT COUNT(1) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'overhead_pool' AND column_name = 'customer_id');
SET @sql := IF(@tbl > 0 AND @col = 0, 'ALTER TABLE overhead_pool ADD COLUMN customer_id CHAR(36) NULL', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
UPDATE overhead_pool SET customer_id = @default_customer_id WHERE customer_id IS NULL;
SET @sql := IF(@tbl > 0, 'ALTER TABLE overhead_pool MODIFY COLUMN customer_id CHAR(36) NOT NULL', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @idx := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'overhead_pool' AND index_name = 'uk_oh_customer_code_version');
SET @sql := IF(@tbl > 0 AND @idx = 0, 'CREATE UNIQUE INDEX uk_oh_customer_code_version ON overhead_pool (customer_id, code, version_number)', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
-- drop global unique if present
SET @idx := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'overhead_pool' AND index_name = 'uk_overhead_code_version');
SET @sql := IF(@idx > 0, 'ALTER TABLE overhead_pool DROP INDEX uk_overhead_code_version', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
