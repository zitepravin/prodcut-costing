-- V135: customer_id on material_master, machine_master, labor_master (TENANT-OWNED per classification)
SET @default_customer_id := (
    SELECT customer_id FROM platform_customer WHERE customer_code = 'DEFAULT' LIMIT 1
);
SET @default_customer_id := IFNULL(@default_customer_id, '00000000-0000-0000-0000-000000000001');

-- material_master
SET @tbl := (SELECT COUNT(1) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'material_master');
SET @col := (SELECT COUNT(1) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'material_master' AND column_name = 'customer_id');
SET @sql := IF(@tbl > 0 AND @col = 0, 'ALTER TABLE material_master ADD COLUMN customer_id CHAR(36) NULL', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
UPDATE material_master SET customer_id = @default_customer_id WHERE customer_id IS NULL;
SET @sql := IF(@tbl > 0, 'ALTER TABLE material_master MODIFY COLUMN customer_id CHAR(36) NOT NULL', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @idx := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'material_master' AND index_name = 'idx_mm_customer');
SET @sql := IF(@tbl > 0 AND @idx = 0, 'CREATE INDEX idx_mm_customer ON material_master (customer_id)', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
-- per-tenant code uniqueness preferred over global; drop global uk if present is ops-sensitive — add composite index
SET @idx := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'material_master' AND index_name = 'idx_mm_customer_code');
SET @sql := IF(@tbl > 0 AND @idx = 0, 'CREATE INDEX idx_mm_customer_code ON material_master (customer_id, code)', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- machine_master
SET @tbl := (SELECT COUNT(1) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'machine_master');
SET @col := (SELECT COUNT(1) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'machine_master' AND column_name = 'customer_id');
SET @sql := IF(@tbl > 0 AND @col = 0, 'ALTER TABLE machine_master ADD COLUMN customer_id CHAR(36) NULL', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
UPDATE machine_master SET customer_id = @default_customer_id WHERE customer_id IS NULL;
SET @sql := IF(@tbl > 0, 'ALTER TABLE machine_master MODIFY COLUMN customer_id CHAR(36) NOT NULL', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @idx := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'machine_master' AND index_name = 'idx_mach_customer');
SET @sql := IF(@tbl > 0 AND @idx = 0, 'CREATE INDEX idx_mach_customer ON machine_master (customer_id)', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- labor_master
SET @tbl := (SELECT COUNT(1) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'labor_master');
SET @col := (SELECT COUNT(1) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'labor_master' AND column_name = 'customer_id');
SET @sql := IF(@tbl > 0 AND @col = 0, 'ALTER TABLE labor_master ADD COLUMN customer_id CHAR(36) NULL', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
UPDATE labor_master SET customer_id = @default_customer_id WHERE customer_id IS NULL;
SET @sql := IF(@tbl > 0, 'ALTER TABLE labor_master MODIFY COLUMN customer_id CHAR(36) NOT NULL', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @idx := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'labor_master' AND index_name = 'idx_labor_customer');
SET @sql := IF(@tbl > 0 AND @idx = 0, 'CREATE INDEX idx_labor_customer ON labor_master (customer_id)', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
