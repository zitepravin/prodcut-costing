-- V134: customer_id on warranty/guarantee/policy/after-sales evidence tables
SET @default_customer_id := (
    SELECT customer_id FROM platform_customer WHERE customer_code = 'DEFAULT' LIMIT 1
);
SET @default_customer_id := IFNULL(@default_customer_id, '00000000-0000-0000-0000-000000000001');

-- helper macro pattern per table
SET @tbl := (SELECT COUNT(1) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'supplier_warranty_claim');
SET @col := (SELECT COUNT(1) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'supplier_warranty_claim' AND column_name = 'customer_id');
SET @sql := IF(@tbl > 0 AND @col = 0, 'ALTER TABLE supplier_warranty_claim ADD COLUMN customer_id CHAR(36) NULL', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
UPDATE supplier_warranty_claim SET customer_id = @default_customer_id WHERE customer_id IS NULL;
SET @sql := IF(@tbl > 0, 'ALTER TABLE supplier_warranty_claim MODIFY COLUMN customer_id CHAR(36) NOT NULL', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @idx := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'supplier_warranty_claim' AND index_name = 'idx_swc_customer');
SET @sql := IF(@tbl > 0 AND @idx = 0, 'CREATE INDEX idx_swc_customer ON supplier_warranty_claim (customer_id)', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @tbl := (SELECT COUNT(1) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'supplier_guarantee_event');
SET @col := (SELECT COUNT(1) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'supplier_guarantee_event' AND column_name = 'customer_id');
SET @sql := IF(@tbl > 0 AND @col = 0, 'ALTER TABLE supplier_guarantee_event ADD COLUMN customer_id CHAR(36) NULL', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
UPDATE supplier_guarantee_event SET customer_id = @default_customer_id WHERE customer_id IS NULL;
SET @sql := IF(@tbl > 0, 'ALTER TABLE supplier_guarantee_event MODIFY COLUMN customer_id CHAR(36) NOT NULL', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @tbl := (SELECT COUNT(1) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'supplier_warranty_term');
SET @col := (SELECT COUNT(1) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'supplier_warranty_term' AND column_name = 'customer_id');
SET @sql := IF(@tbl > 0 AND @col = 0, 'ALTER TABLE supplier_warranty_term ADD COLUMN customer_id CHAR(36) NULL', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
UPDATE supplier_warranty_term SET customer_id = @default_customer_id WHERE customer_id IS NULL;
SET @sql := IF(@tbl > 0, 'ALTER TABLE supplier_warranty_term MODIFY COLUMN customer_id CHAR(36) NOT NULL', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @tbl := (SELECT COUNT(1) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'supplier_guarantee_term');
SET @col := (SELECT COUNT(1) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'supplier_guarantee_term' AND column_name = 'customer_id');
SET @sql := IF(@tbl > 0 AND @col = 0, 'ALTER TABLE supplier_guarantee_term ADD COLUMN customer_id CHAR(36) NULL', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
UPDATE supplier_guarantee_term SET customer_id = @default_customer_id WHERE customer_id IS NULL;
SET @sql := IF(@tbl > 0, 'ALTER TABLE supplier_guarantee_term MODIFY COLUMN customer_id CHAR(36) NOT NULL', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @tbl := (SELECT COUNT(1) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'supplier_policy_evidence');
SET @col := (SELECT COUNT(1) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'supplier_policy_evidence' AND column_name = 'customer_id');
SET @sql := IF(@tbl > 0 AND @col = 0, 'ALTER TABLE supplier_policy_evidence ADD COLUMN customer_id CHAR(36) NULL', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
UPDATE supplier_policy_evidence SET customer_id = @default_customer_id WHERE customer_id IS NULL;
SET @sql := IF(@tbl > 0, 'ALTER TABLE supplier_policy_evidence MODIFY COLUMN customer_id CHAR(36) NOT NULL', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @tbl := (SELECT COUNT(1) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'supplier_after_sales_metric');
SET @col := (SELECT COUNT(1) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'supplier_after_sales_metric' AND column_name = 'customer_id');
SET @sql := IF(@tbl > 0 AND @col = 0, 'ALTER TABLE supplier_after_sales_metric ADD COLUMN customer_id CHAR(36) NULL', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
UPDATE supplier_after_sales_metric SET customer_id = @default_customer_id WHERE customer_id IS NULL;
SET @sql := IF(@tbl > 0, 'ALTER TABLE supplier_after_sales_metric MODIFY COLUMN customer_id CHAR(36) NOT NULL', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
