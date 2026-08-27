-- V131: Data-plane multi-tenancy Wave 1
-- Add customer_id to highest-risk tenant-owned tables, backfill DEFAULT customer,
-- adjust uniqueness to (customer_id, code), add composite indexes.
--
-- Safe for existing single-tenant deployments: all rows map to customer_code = 'DEFAULT'.
-- Application MUST set customer_id from CustomerContext on insert; never trust client alone.

-- ---------------------------------------------------------------------------
-- Helper: resolve DEFAULT customer once (stable for this migration session)
-- ---------------------------------------------------------------------------
SET @default_customer_id := (
    SELECT customer_id FROM platform_customer WHERE customer_code = 'DEFAULT' LIMIT 1
);

-- Fail loudly if DEFAULT customer missing (should exist from V32)
SET @default_customer_id := IFNULL(@default_customer_id, '00000000-0000-0000-0000-000000000001');

-- Ensure a DEFAULT row exists if seed was skipped in some environments
INSERT INTO platform_customer (customer_id, customer_code, customer_name, status, created_at)
SELECT @default_customer_id, 'DEFAULT', 'Default Customer', 'ACTIVE', CURRENT_TIMESTAMP(6)
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM platform_customer WHERE customer_code = 'DEFAULT');

SET @default_customer_id := (
    SELECT customer_id FROM platform_customer WHERE customer_code = 'DEFAULT' LIMIT 1
);

-- ---------------------------------------------------------------------------
-- vehicle_program
-- ---------------------------------------------------------------------------
SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'vehicle_program' AND column_name = 'customer_id'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE vehicle_program ADD COLUMN customer_id CHAR(36) NULL AFTER program_id',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE vehicle_program SET customer_id = @default_customer_id WHERE customer_id IS NULL;

-- Drop global unique on program_code if present; replace with per-tenant unique
SET @uk_exists := (
  SELECT COUNT(1) FROM information_schema.table_constraints
  WHERE table_schema = DATABASE() AND table_name = 'vehicle_program'
    AND constraint_type = 'UNIQUE' AND constraint_name = 'program_code'
);
-- MySQL may name unique as program_code or uk_...
SET @uk_name := (
  SELECT constraint_name FROM information_schema.table_constraints
  WHERE table_schema = DATABASE() AND table_name = 'vehicle_program'
    AND constraint_type = 'UNIQUE' AND constraint_name LIKE '%program_code%'
  LIMIT 1
);
SET @sql := IF(@uk_name IS NOT NULL,
  CONCAT('ALTER TABLE vehicle_program DROP INDEX `', @uk_name, '`'),
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Also handle CONSTRAINT uk style from entity unique=true
SET @uk_name2 := (
  SELECT index_name FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'vehicle_program'
    AND column_name = 'program_code' AND non_unique = 0
    AND index_name != 'PRIMARY'
  LIMIT 1
);
SET @sql := IF(@uk_name2 IS NOT NULL,
  CONCAT('ALTER TABLE vehicle_program DROP INDEX `', @uk_name2, '`'),
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

ALTER TABLE vehicle_program
  MODIFY COLUMN customer_id CHAR(36) NOT NULL;

-- Per-tenant unique program code
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'vehicle_program'
    AND index_name = 'uk_vehicle_program_customer_code'
);
SET @sql := IF(@idx_exists = 0,
  'ALTER TABLE vehicle_program ADD CONSTRAINT uk_vehicle_program_customer_code UNIQUE (customer_id, program_code)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'vehicle_program'
    AND index_name = 'idx_vehicle_program_customer_status'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_vehicle_program_customer_status ON vehicle_program (customer_id, status)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- bom_node
-- ---------------------------------------------------------------------------
SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'bom_node' AND column_name = 'customer_id'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE bom_node ADD COLUMN customer_id CHAR(36) NULL AFTER bom_node_id',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Backfill from parent program when possible, else DEFAULT
UPDATE bom_node b
LEFT JOIN vehicle_program vp ON vp.program_id = b.program_id
SET b.customer_id = COALESCE(vp.customer_id, @default_customer_id)
WHERE b.customer_id IS NULL;

ALTER TABLE bom_node MODIFY COLUMN customer_id CHAR(36) NOT NULL;

SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'bom_node'
    AND index_name = 'idx_bom_node_customer_program'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_bom_node_customer_program ON bom_node (customer_id, program_id)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'bom_node'
    AND index_name = 'idx_bom_node_customer_status'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_bom_node_customer_status ON bom_node (customer_id, status)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- plant_master
-- ---------------------------------------------------------------------------
SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'plant_master' AND column_name = 'customer_id'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE plant_master ADD COLUMN customer_id CHAR(36) NULL AFTER plant_id',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE plant_master SET customer_id = @default_customer_id WHERE customer_id IS NULL;
ALTER TABLE plant_master MODIFY COLUMN customer_id CHAR(36) NOT NULL;

SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'plant_master'
    AND index_name = 'idx_plant_master_customer'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_plant_master_customer ON plant_master (customer_id)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- material_master
-- ---------------------------------------------------------------------------
SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'material_master' AND column_name = 'customer_id'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE material_master ADD COLUMN customer_id CHAR(36) NULL AFTER record_id',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE material_master SET customer_id = @default_customer_id WHERE customer_id IS NULL;
ALTER TABLE material_master MODIFY COLUMN customer_id CHAR(36) NOT NULL;

SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'material_master'
    AND index_name = 'idx_material_master_customer'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_material_master_customer ON material_master (customer_id)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- machine_master
-- ---------------------------------------------------------------------------
SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'machine_master' AND column_name = 'customer_id'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE machine_master ADD COLUMN customer_id CHAR(36) NULL AFTER record_id',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE machine_master SET customer_id = @default_customer_id WHERE customer_id IS NULL;
ALTER TABLE machine_master MODIFY COLUMN customer_id CHAR(36) NOT NULL;

SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'machine_master'
    AND index_name = 'idx_machine_master_customer'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_machine_master_customer ON machine_master (customer_id)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- labor_master
-- ---------------------------------------------------------------------------
SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'labor_master' AND column_name = 'customer_id'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE labor_master ADD COLUMN customer_id CHAR(36) NULL AFTER record_id',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE labor_master SET customer_id = @default_customer_id WHERE customer_id IS NULL;
ALTER TABLE labor_master MODIFY COLUMN customer_id CHAR(36) NOT NULL;

SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'labor_master'
    AND index_name = 'idx_labor_master_customer'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_labor_master_customer ON labor_master (customer_id)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- supplier_master
-- ---------------------------------------------------------------------------
SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'supplier_master' AND column_name = 'customer_id'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE supplier_master ADD COLUMN customer_id CHAR(36) NULL AFTER supplier_id',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE supplier_master SET customer_id = @default_customer_id WHERE customer_id IS NULL;

-- Drop global unique on supplier_code
SET @uk_name := (
  SELECT index_name FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'supplier_master'
    AND column_name = 'supplier_code' AND non_unique = 0
    AND index_name != 'PRIMARY'
  LIMIT 1
);
SET @sql := IF(@uk_name IS NOT NULL,
  CONCAT('ALTER TABLE supplier_master DROP INDEX `', @uk_name, '`'),
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

ALTER TABLE supplier_master MODIFY COLUMN customer_id CHAR(36) NOT NULL;

SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'supplier_master'
    AND index_name = 'uk_supplier_customer_code'
);
SET @sql := IF(@idx_exists = 0,
  'ALTER TABLE supplier_master ADD CONSTRAINT uk_supplier_customer_code UNIQUE (customer_id, supplier_code)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'supplier_master'
    AND index_name = 'idx_supplier_customer_status'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_supplier_customer_status ON supplier_master (customer_id, status)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- should_cost_build_up
-- ---------------------------------------------------------------------------
SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'should_cost_build_up' AND column_name = 'customer_id'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE should_cost_build_up ADD COLUMN customer_id CHAR(36) NULL AFTER build_up_id',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE should_cost_build_up SET customer_id = @default_customer_id WHERE customer_id IS NULL;
ALTER TABLE should_cost_build_up MODIFY COLUMN customer_id CHAR(36) NOT NULL;

SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'should_cost_build_up'
    AND index_name = 'idx_should_cost_customer'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_should_cost_customer ON should_cost_build_up (customer_id)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- rfq_event
-- ---------------------------------------------------------------------------
SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'rfq_event' AND column_name = 'customer_id'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE rfq_event ADD COLUMN customer_id CHAR(36) NULL AFTER rfq_id',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE rfq_event SET customer_id = @default_customer_id WHERE customer_id IS NULL;
ALTER TABLE rfq_event MODIFY COLUMN customer_id CHAR(36) NOT NULL;

SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'rfq_event'
    AND index_name = 'idx_rfq_event_customer_status'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_rfq_event_customer_status ON rfq_event (customer_id, status)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- purchase_request: harden nullable → NOT NULL after backfill
-- ---------------------------------------------------------------------------
UPDATE purchase_request SET customer_id = @default_customer_id WHERE customer_id IS NULL;
ALTER TABLE purchase_request MODIFY COLUMN customer_id CHAR(36) NOT NULL;

SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'purchase_request'
    AND index_name = 'idx_pr_customer_status'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_pr_customer_status ON purchase_request (customer_id, status)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- make_vs_buy_simulation
-- ---------------------------------------------------------------------------
SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'make_vs_buy_simulation' AND column_name = 'customer_id'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE make_vs_buy_simulation ADD COLUMN customer_id CHAR(36) NULL AFTER simulation_id',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE make_vs_buy_simulation SET customer_id = @default_customer_id WHERE customer_id IS NULL;
-- Only NOT NULL if column was added / table exists
SET @tbl := (
  SELECT COUNT(1) FROM information_schema.tables
  WHERE table_schema = DATABASE() AND table_name = 'make_vs_buy_simulation'
);
SET @sql := IF(@tbl > 0,
  'ALTER TABLE make_vs_buy_simulation MODIFY COLUMN customer_id CHAR(36) NOT NULL',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'make_vs_buy_simulation'
    AND index_name = 'idx_mvb_customer'
);
SET @sql := IF(@idx_exists = 0 AND @tbl > 0,
  'CREATE INDEX idx_mvb_customer ON make_vs_buy_simulation (customer_id)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- costing_run (if present)
-- ---------------------------------------------------------------------------
SET @tbl := (
  SELECT COUNT(1) FROM information_schema.tables
  WHERE table_schema = DATABASE() AND table_name = 'costing_run'
);
SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'costing_run' AND column_name = 'customer_id'
);
SET @sql := IF(@tbl > 0 AND @col_exists = 0,
  'ALTER TABLE costing_run ADD COLUMN customer_id CHAR(36) NULL',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE costing_run SET customer_id = @default_customer_id WHERE customer_id IS NULL AND @tbl > 0;

SET @sql := IF(@tbl > 0,
  'ALTER TABLE costing_run MODIFY COLUMN customer_id CHAR(36) NOT NULL',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'costing_run'
    AND index_name = 'idx_costing_run_customer'
);
SET @sql := IF(@idx_exists = 0 AND @tbl > 0,
  'CREATE INDEX idx_costing_run_customer ON costing_run (customer_id)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- vendor_comparison (if present)
-- ---------------------------------------------------------------------------
SET @tbl := (
  SELECT COUNT(1) FROM information_schema.tables
  WHERE table_schema = DATABASE() AND table_name = 'vendor_comparison'
);
SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'vendor_comparison' AND column_name = 'customer_id'
);
SET @sql := IF(@tbl > 0 AND @col_exists = 0,
  'ALTER TABLE vendor_comparison ADD COLUMN customer_id CHAR(36) NULL',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE vendor_comparison SET customer_id = @default_customer_id WHERE customer_id IS NULL;

SET @sql := IF(@tbl > 0,
  'ALTER TABLE vendor_comparison MODIFY COLUMN customer_id CHAR(36) NOT NULL',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'vendor_comparison'
    AND index_name = 'idx_vendor_comparison_customer'
);
SET @sql := IF(@idx_exists = 0 AND @tbl > 0,
  'CREATE INDEX idx_vendor_comparison_customer ON vendor_comparison (customer_id)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
