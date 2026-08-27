-- V137: per-tenant uniqueness for masters; questionnaire response/evaluation customer_id
SET @default_customer_id := (
    SELECT customer_id FROM platform_customer WHERE customer_code = 'DEFAULT' LIMIT 1
);
SET @default_customer_id := IFNULL(@default_customer_id, '00000000-0000-0000-0000-000000000001');

-- Drop global unique (code, version) and add (customer_id, code, version) when safe
-- material_master
SET @idx := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'material_master' AND index_name = 'uk_material_code_version');
SET @sql := IF(@idx > 0, 'ALTER TABLE material_master DROP INDEX uk_material_code_version', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @idx := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'material_master' AND index_name = 'uk_mm_customer_code_version');
SET @sql := IF(@idx = 0, 'CREATE UNIQUE INDEX uk_mm_customer_code_version ON material_master (customer_id, code, version_number)', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- machine_master
SET @idx := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'machine_master' AND index_name = 'uk_machine_code_version');
SET @sql := IF(@idx > 0, 'ALTER TABLE machine_master DROP INDEX uk_machine_code_version', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @idx := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'machine_master' AND index_name = 'uk_mach_customer_code_version');
SET @sql := IF(@idx = 0, 'CREATE UNIQUE INDEX uk_mach_customer_code_version ON machine_master (customer_id, code, version_number)', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- labor_master
SET @idx := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'labor_master' AND index_name = 'uk_labor_code_version');
SET @sql := IF(@idx > 0, 'ALTER TABLE labor_master DROP INDEX uk_labor_code_version', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @idx := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'labor_master' AND index_name = 'uk_labor_customer_code_version');
SET @sql := IF(@idx = 0, 'CREATE UNIQUE INDEX uk_labor_customer_code_version ON labor_master (customer_id, code, version_number)', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- process_master: drop unique on process_name if present (index name may vary)
SET @idx_name := (
  SELECT index_name FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'process_master'
    AND column_name = 'process_name' AND non_unique = 0 AND index_name <> 'PRIMARY' LIMIT 1);
SET @sql := IF(@idx_name IS NOT NULL, CONCAT('ALTER TABLE process_master DROP INDEX `', @idx_name, '`'), 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @idx := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'process_master' AND index_name = 'uk_pm_customer_process_name');
SET @col := (SELECT COUNT(1) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'process_master' AND column_name = 'process_name');
SET @sql := IF(@idx = 0 AND @col > 0, 'CREATE UNIQUE INDEX uk_pm_customer_process_name ON process_master (customer_id, process_name)', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
-- questionnaire response
SET @tbl := (SELECT COUNT(1) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'vendor_questionnaire_response');
SET @col := (SELECT COUNT(1) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'vendor_questionnaire_response' AND column_name = 'customer_id');
SET @sql := IF(@tbl > 0 AND @col = 0, 'ALTER TABLE vendor_questionnaire_response ADD COLUMN customer_id CHAR(36) NULL', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
UPDATE vendor_questionnaire_response SET customer_id = @default_customer_id WHERE customer_id IS NULL;
SET @sql := IF(@tbl > 0, 'ALTER TABLE vendor_questionnaire_response MODIFY COLUMN customer_id CHAR(36) NOT NULL', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- questionnaire evaluation
SET @tbl := (SELECT COUNT(1) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'vendor_questionnaire_evaluation');
SET @col := (SELECT COUNT(1) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'vendor_questionnaire_evaluation' AND column_name = 'customer_id');
SET @sql := IF(@tbl > 0 AND @col = 0, 'ALTER TABLE vendor_questionnaire_evaluation ADD COLUMN customer_id CHAR(36) NULL', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
UPDATE vendor_questionnaire_evaluation SET customer_id = @default_customer_id WHERE customer_id IS NULL;
SET @sql := IF(@tbl > 0, 'ALTER TABLE vendor_questionnaire_evaluation MODIFY COLUMN customer_id CHAR(36) NOT NULL', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
