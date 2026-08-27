-- =============================================================================
-- V167 Complete Demo Company Data â€” extends V18 + V166 (does NOT replace)
-- Password for all demo users: ChangeMe!Now1
-- BCrypt: $2b$10$TWZ.5vAxuEf/dDNqxblzr.JrLtk2NG0I63008RTvJbZOnGkEIV0pe
-- Idempotent: fixed UUIDs + WHERE NOT EXISTS / INSERT IGNORE
--
-- PRESERVES existing:
--   DEFAULT tenant, EV-CX-2027, CRS-1010, AL-6061, PRESS-800T, OP-STAMP,
--   OH-MFG, CA-LH-FR MAKE, HUB-BRG-FR BUY, ACME, NIPPON, RFQ, should-cost, target
--
-- ADDS:
--   TENANT-A Aurora Automotive Systems (primary rich demo)
--   TENANT-B Horizon Mobility Components (isolation demo)
--   Extra buyers/engineers for workload
--   Additional products, BOMs, routings, RFQs, PRs, POs, tooling, tickets,
--   FX rates, approval states, incomplete masters for dashboard attention items
-- =============================================================================

-- Fixed ID ranges used in this file (avoid collision with V18 a/b/c/d/e/f1000â€¦):
--   Customers     g1000000-0000-4000-8000-00000000000x
--   Extra users   f1000000-0000-4000-8000-00000000002x .. 03x
--   Plants        a1000000-0000-4000-8000-00000000001x
--   Programs      a2000000-0000-4000-8000-00000000001x
--   Materials     a3000000-0000-4000-8000-00000000001x
--   Machines      a4000000-0000-4000-8000-00000000001x
--   Labor         a5000000-0000-4000-8000-00000000001x
--   Overhead      a6000000-0000-4000-8000-00000000001x
--   BOM nodes     b1000000-0000-4000-8000-00000000001x
--   Suppliers     c1000000-0000-4000-8000-00000000001x
--   RFQ / Quote   d1000000-0000-4000-8000-00000000001x
--   Costing       e1000000-0000-4000-8000-00000000001x
--   PR / PO       p1000000-0000-4000-8000-00000000000x
--   Tooling       t1000000-0000-4000-8000-00000000000x
--   Tickets       s1000000-0000-4000-8000-00000000000x

-- ---------------------------------------------------------------------------
-- 1. TENANTS (Customer A / Customer B)
-- ---------------------------------------------------------------------------
INSERT INTO platform_customer (customer_id, customer_code, customer_name, status, created_at)
SELECT 'g1000000-0000-4000-8000-000000000001', 'AURORA', 'Aurora Automotive Systems', 'ACTIVE', UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM platform_customer WHERE customer_code = 'AURORA');

INSERT INTO platform_customer (customer_id, customer_code, customer_name, status, created_at)
SELECT 'g1000000-0000-4000-8000-000000000002', 'HORIZON', 'Horizon Mobility Components', 'ACTIVE', UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM platform_customer WHERE customer_code = 'HORIZON');

-- Module entitlements for AURORA (full stack)
INSERT INTO customer_module_entitlement (entitlement_id, customer_id, module_id, enabled, created_at)
SELECT UUID(), c.customer_id, m.module_id, TRUE, UTC_TIMESTAMP(6)
FROM platform_customer c
CROSS JOIN platform_module m
WHERE c.customer_code = 'AURORA'
  AND m.module_code IN ('PRODUCT_COSTING','VENDOR_SELECTION','PURCHASE','SUPPORT')
  AND NOT EXISTS (
      SELECT 1 FROM customer_module_entitlement e
      WHERE e.customer_id = c.customer_id AND e.module_id = m.module_id
  );

-- Module entitlements for HORIZON (costing + purchase only â€” isolation demo)
INSERT INTO customer_module_entitlement (entitlement_id, customer_id, module_id, enabled, created_at)
SELECT UUID(), c.customer_id, m.module_id, TRUE, UTC_TIMESTAMP(6)
FROM platform_customer c
CROSS JOIN platform_module m
WHERE c.customer_code = 'HORIZON'
  AND m.module_code IN ('PRODUCT_COSTING','PURCHASE')
  AND NOT EXISTS (
      SELECT 1 FROM customer_module_entitlement e
      WHERE e.customer_id = c.customer_id AND e.module_id = m.module_id
  );

-- Ensure DEFAULT also has core modules (idempotent)
INSERT INTO customer_module_entitlement (entitlement_id, customer_id, module_id, enabled, created_at)
SELECT UUID(), c.customer_id, m.module_id, TRUE, UTC_TIMESTAMP(6)
FROM platform_customer c
CROSS JOIN platform_module m
WHERE c.customer_code = 'DEFAULT'
  AND m.module_code IN ('PRODUCT_COSTING','VENDOR_SELECTION','PURCHASE','SUPPORT')
  AND NOT EXISTS (
      SELECT 1 FROM customer_module_entitlement e
      WHERE e.customer_id = c.customer_id AND e.module_id = m.module_id
  );

-- ---------------------------------------------------------------------------
-- 2. EXTRA DEMO USERS (workload / team scenarios)
-- ---------------------------------------------------------------------------
-- Additional buyers (manager dashboard workload)
INSERT INTO app_user (user_id, username, email, display_name, password_hash, status, mfa_enabled,
    token_version, failed_login_attempts, password_updated_at, created_at)
SELECT 'f1000000-0000-4000-8000-000000000021', 'demo.buyer2', 'buyer2@demo.auto',
       'Demo Buyer Two',
       '$2b$10$TWZ.5vAxuEf/dDNqxblzr.JrLtk2NG0I63008RTvJbZOnGkEIV0pe',
       'Active', FALSE, 0, 0, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'demo.buyer2');

INSERT INTO app_user (user_id, username, email, display_name, password_hash, status, mfa_enabled,
    token_version, failed_login_attempts, password_updated_at, created_at)
SELECT 'f1000000-0000-4000-8000-000000000022', 'demo.buyer3', 'buyer3@demo.auto',
       'Demo Buyer Three',
       '$2b$10$TWZ.5vAxuEf/dDNqxblzr.JrLtk2NG0I63008RTvJbZOnGkEIV0pe',
       'Active', FALSE, 0, 0, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'demo.buyer3');

-- Additional costing engineers
INSERT INTO app_user (user_id, username, email, display_name, password_hash, status, mfa_enabled,
    token_version, failed_login_attempts, password_updated_at, created_at)
SELECT 'f1000000-0000-4000-8000-000000000023', 'demo.engineer2', 'engineer2@demo.auto',
       'Demo Costing Engineer Two',
       '$2b$10$TWZ.5vAxuEf/dDNqxblzr.JrLtk2NG0I63008RTvJbZOnGkEIV0pe',
       'Active', FALSE, 0, 0, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'demo.engineer2');

-- Horizon (Tenant B) users
INSERT INTO app_user (user_id, username, email, display_name, password_hash, status, mfa_enabled,
    token_version, failed_login_attempts, password_updated_at, created_at)
SELECT 'f1000000-0000-4000-8000-000000000031', 'horizon.admin', 'admin@horizon.demo',
       'Horizon Customer Admin',
       '$2b$10$TWZ.5vAxuEf/dDNqxblzr.JrLtk2NG0I63008RTvJbZOnGkEIV0pe',
       'Active', FALSE, 0, 0, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'horizon.admin');

INSERT INTO app_user (user_id, username, email, display_name, password_hash, status, mfa_enabled,
    token_version, failed_login_attempts, password_updated_at, created_at)
SELECT 'f1000000-0000-4000-8000-000000000032', 'horizon.engineer', 'engineer@horizon.demo',
       'Horizon Costing Engineer',
       '$2b$10$TWZ.5vAxuEf/dDNqxblzr.JrLtk2NG0I63008RTvJbZOnGkEIV0pe',
       'Active', FALSE, 0, 0, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'horizon.engineer');

INSERT INTO app_user (user_id, username, email, display_name, password_hash, status, mfa_enabled,
    token_version, failed_login_attempts, password_updated_at, created_at)
SELECT 'f1000000-0000-4000-8000-000000000033', 'horizon.buyer', 'buyer@horizon.demo',
       'Horizon Purchasing Buyer',
       '$2b$10$TWZ.5vAxuEf/dDNqxblzr.JrLtk2NG0I63008RTvJbZOnGkEIV0pe',
       'Active', FALSE, 0, 0, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'horizon.buyer');

-- Aurora custom roles users
INSERT INTO app_user (user_id, username, email, display_name, password_hash, status, mfa_enabled,
    token_version, failed_login_attempts, password_updated_at, created_at)
SELECT 'f1000000-0000-4000-8000-000000000024', 'demo.proccoord', 'proccoord@demo.auto',
       'Demo Procurement Coordinator',
       '$2b$10$TWZ.5vAxuEf/dDNqxblzr.JrLtk2NG0I63008RTvJbZOnGkEIV0pe',
       'Active', FALSE, 0, 0, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'demo.proccoord');

INSERT INTO app_user (user_id, username, email, display_name, password_hash, status, mfa_enabled,
    token_version, failed_login_attempts, password_updated_at, created_at)
SELECT 'f1000000-0000-4000-8000-000000000025', 'demo.costanalyst', 'costanalyst@demo.auto',
       'Demo Costing Analyst',
       '$2b$10$TWZ.5vAxuEf/dDNqxblzr.JrLtk2NG0I63008RTvJbZOnGkEIV0pe',
       'Active', FALSE, 0, 0, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'demo.costanalyst');

-- Role assignments for extra users
INSERT IGNORE INTO user_role (user_id, role_id)
SELECT u.user_id, r.role_id FROM app_user u, role r
WHERE u.username = 'demo.buyer2' AND r.role_code = 'PURCHASING_BUYER';

INSERT IGNORE INTO user_role (user_id, role_id)
SELECT u.user_id, r.role_id FROM app_user u, role r
WHERE u.username = 'demo.buyer3' AND r.role_code = 'PURCHASING_BUYER';

INSERT IGNORE INTO user_role (user_id, role_id)
SELECT u.user_id, r.role_id FROM app_user u, role r
WHERE u.username = 'demo.engineer2' AND r.role_code = 'COSTING_ENGINEER';

INSERT IGNORE INTO user_role (user_id, role_id)
SELECT u.user_id, r.role_id FROM app_user u, role r
WHERE u.username = 'horizon.admin' AND r.role_code = 'CUSTOMER_ADMIN';

INSERT IGNORE INTO user_role (user_id, role_id)
SELECT u.user_id, r.role_id FROM app_user u, role r
WHERE u.username = 'horizon.engineer' AND r.role_code = 'COSTING_ENGINEER';

INSERT IGNORE INTO user_role (user_id, role_id)
SELECT u.user_id, r.role_id FROM app_user u, role r
WHERE u.username = 'horizon.buyer' AND r.role_code = 'PURCHASING_BUYER';

INSERT IGNORE INTO user_role (user_id, role_id)
SELECT u.user_id, r.role_id FROM app_user u, role r
WHERE u.username = 'demo.proccoord' AND r.role_code = 'PURCHASING_BUYER';

INSERT IGNORE INTO user_role (user_id, role_id)
SELECT u.user_id, r.role_id FROM app_user u, role r
WHERE u.username = 'demo.costanalyst' AND r.role_code = 'COSTING_ENGINEER';

-- Memberships: DEFAULT (existing personas + extras)
INSERT INTO customer_user_membership (membership_id, customer_id, user_id, status, created_at)
SELECT UUID(), c.customer_id, u.user_id, 'ACTIVE', UTC_TIMESTAMP(6)
FROM platform_customer c
CROSS JOIN app_user u
WHERE c.customer_code = 'DEFAULT'
  AND u.username IN (
      'demo.buyer2','demo.buyer3','demo.engineer2',
      'demo.proccoord','demo.costanalyst'
  )
  AND NOT EXISTS (
      SELECT 1 FROM customer_user_membership m
      WHERE m.customer_id = c.customer_id AND m.user_id = u.user_id
  );

-- Memberships: AURORA (mirror of primary demo users for Tenant-A isolation testing)
INSERT INTO customer_user_membership (membership_id, customer_id, user_id, status, created_at)
SELECT UUID(), c.customer_id, u.user_id, 'ACTIVE', UTC_TIMESTAMP(6)
FROM platform_customer c
CROSS JOIN app_user u
WHERE c.customer_code = 'AURORA'
  AND u.username IN (
      'demo.systemadmin','demo.finance','demo.sourcing','demo.programcost',
      'demo.process','demo.tooling','demo.materialbuyer','demo.industrial',
      'demo.productcost','demo.purchasemanager','demo.platformowner','demo.customeradmin',
      'demo.engineer','demo.manager','demo.buyer','demo.master',
      'demo.buyer2','demo.buyer3','demo.engineer2','demo.proccoord','demo.costanalyst'
  )
  AND NOT EXISTS (
      SELECT 1 FROM customer_user_membership m
      WHERE m.customer_id = c.customer_id AND m.user_id = u.user_id
  );

-- Memberships: HORIZON only
INSERT INTO customer_user_membership (membership_id, customer_id, user_id, status, created_at)
SELECT UUID(), c.customer_id, u.user_id, 'ACTIVE', UTC_TIMESTAMP(6)
FROM platform_customer c
CROSS JOIN app_user u
WHERE c.customer_code = 'HORIZON'
  AND u.username IN ('horizon.admin','horizon.engineer','horizon.buyer')
  AND NOT EXISTS (
      SELECT 1 FROM customer_user_membership m
      WHERE m.customer_id = c.customer_id AND m.user_id = u.user_id
  );

-- ---------------------------------------------------------------------------
-- 3. ADDITIONAL PLANTS / MATERIALS / MACHINES / LABOR / OVERHEAD
-- Schema-aligned to AbstractMasterRateRecord + MaterialMaster entity
-- customer_id resolved from platform_customer (DEFAULT used where tenant not yet set)
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO plant_master (plant_id, plant_code, country, statutory_duty_notes, freight_zone)
VALUES
('a1000000-0000-4000-8000-000000000011', 'AUR-DET', 'US', 'Aurora Detroit stamping', 'NA-EAST'),
('a1000000-0000-4000-8000-000000000012', 'AUR-PUN', 'IN', 'Aurora Pune machining', 'APAC-IN'),
('a1000000-0000-4000-8000-000000000021', 'HOR-BLR', 'IN', 'Horizon Bangalore', 'APAC-IN');

-- Resolve tenant IDs
SET @cust_default := (SELECT customer_id FROM platform_customer WHERE customer_code = 'DEFAULT' LIMIT 1);
SET @cust_aurora  := (SELECT customer_id FROM platform_customer WHERE customer_code = 'AURORA' LIMIT 1);
SET @cust_horizon := (SELECT customer_id FROM platform_customer WHERE customer_code = 'HORIZON' LIMIT 1);
SET @cust_default := IFNULL(@cust_default, '00000000-0000-0000-0000-000000000001');
SET @cust_aurora  := IFNULL(@cust_aurora, @cust_default);
SET @cust_horizon := IFNULL(@cust_horizon, @cust_default);

-- Materials (correct columns: record_id, customer_id, code, rate, ...)
INSERT INTO material_master (
    record_id, customer_id, code, description, rate, uom, plant_id,
    effective_from, effective_to, version_number, status,
    grade, specification_standard, density, scrap_recovery_rate,
    material_form, index_linked_flag, created_at
)
SELECT 'a3000000-0000-4000-8000-000000000011', @cust_aurora, 'HSLA-550',
       'HSLA steel 550 MPa coil', 1.150000, 'KG', 'a1000000-0000-4000-8000-000000000011',
       '2025-01-01', NULL, 1, 'ACTIVE',
       'HSLA-550', 'EN 10149', 7.850000, 0.120000, 'COIL', FALSE, UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM material_master WHERE code = 'HSLA-550' AND customer_id = @cust_aurora);

INSERT INTO material_master (
    record_id, customer_id, code, description, rate, uom, plant_id,
    effective_from, effective_to, version_number, status,
    grade, specification_standard, density, scrap_recovery_rate,
    material_form, index_linked_flag, created_at
)
SELECT 'a3000000-0000-4000-8000-000000000012', @cust_aurora, 'NYLON-GF30',
       'Nylon 6 GF30 pellet', 4.250000, 'KG', 'a1000000-0000-4000-8000-000000000011',
       '2025-01-01', NULL, 1, 'ACTIVE',
       'PA6-GF30', 'ISO 527', 1.350000, 0.050000, 'PELLET', FALSE, UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM material_master WHERE code = 'NYLON-GF30' AND customer_id = @cust_aurora);

INSERT INTO material_master (
    record_id, customer_id, code, description, rate, uom, plant_id,
    effective_from, effective_to, version_number, status,
    grade, specification_standard, density, scrap_recovery_rate,
    material_form, index_linked_flag, created_at
)
SELECT 'a3000000-0000-4000-8000-000000000013', @cust_aurora, 'INCOMPLETE-MAT-01',
       'Incomplete material â€” missing rate context', 0.000001, 'KG', NULL,
       '2025-01-01', NULL, 1, 'DRAFT',
       NULL, NULL, NULL, NULL, 'SHEET', FALSE, UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM material_master WHERE code = 'INCOMPLETE-MAT-01' AND customer_id = @cust_aurora);

INSERT INTO machine_master (
    record_id, customer_id, code, description, rate, uom, plant_id,
    effective_from, version_number, status,
    machine_type, capacity, depreciation_life_years, capital_cost, annual_operating_hours,
    power_cost_per_hour, maintenance_cost_per_hour, floor_space_cost_per_hour, shift_pattern, created_at
)
SELECT 'a4000000-0000-4000-8000-000000000011', @cust_aurora, 'CNC-5AXIS-01',
       '5-axis CNC machining center', 95.000000, 'HR', 'a1000000-0000-4000-8000-000000000012',
       '2025-01-01', 1, 'ACTIVE',
       'CNC', '5-AXIS', 10.00, 850000.000000, 4000.00,
       12.000000, 8.000000, 4.000000, '2-SHIFT', UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM machine_master WHERE code = 'CNC-5AXIS-01' AND customer_id = @cust_aurora);

INSERT INTO machine_master (
    record_id, customer_id, code, description, rate, uom, plant_id,
    effective_from, version_number, status,
    machine_type, capacity, depreciation_life_years, capital_cost, annual_operating_hours,
    power_cost_per_hour, maintenance_cost_per_hour, floor_space_cost_per_hour, shift_pattern, created_at
)
SELECT 'a4000000-0000-4000-8000-000000000012', @cust_aurora, 'INJ-250T',
       '250T injection molding press', 62.500000, 'HR', 'a1000000-0000-4000-8000-000000000011',
       '2025-01-01', 1, 'ACTIVE',
       'INJECTION', '250T', 8.00, 420000.000000, 4800.00,
       15.000000, 6.000000, 3.500000, '3-SHIFT', UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM machine_master WHERE code = 'INJ-250T' AND customer_id = @cust_aurora);

INSERT INTO labor_master (
    record_id, customer_id, code, description, rate, uom, plant_id,
    effective_from, version_number, status,
    operator_grade, shift_type, country_region, statutory_burden_pct, created_at
)
SELECT 'a5000000-0000-4000-8000-000000000011', @cust_aurora, 'OP-CNC',
       'CNC machine operator', 32.000000, 'HR', 'a1000000-0000-4000-8000-000000000012',
       '2025-01-01', 1, 'ACTIVE',
       'GRADE_A', 'DAY', 'IN-MH', 0.280000, UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM labor_master WHERE code = 'OP-CNC' AND customer_id = @cust_aurora);

INSERT INTO labor_master (
    record_id, customer_id, code, description, rate, uom, plant_id,
    effective_from, version_number, status,
    operator_grade, shift_type, country_region, statutory_burden_pct, created_at
)
SELECT 'a5000000-0000-4000-8000-000000000012', @cust_aurora, 'OP-MOLD',
       'Injection mold operator', 24.500000, 'HR', 'a1000000-0000-4000-8000-000000000011',
       '2025-01-01', 1, 'ACTIVE',
       'GRADE_B', 'DAY', 'US-MI', 0.350000, UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM labor_master WHERE code = 'OP-MOLD' AND customer_id = @cust_aurora);

INSERT INTO overhead_pool (
    record_id, customer_id, code, description, rate, uom, plant_id,
    effective_from, version_number, status,
    pool_category, allocation_base, period, created_at
)
SELECT 'a6000000-0000-4000-8000-000000000011', @cust_aurora, 'OH-AUR-MFG',
       'Aurora manufacturing overhead', 38.000000, 'HR', 'a1000000-0000-4000-8000-000000000011',
       '2025-01-01', 1, 'ACTIVE',
       'MFG', 'DIRECT_LABOR_HOURS', '2026', UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM overhead_pool WHERE code = 'OH-AUR-MFG' AND customer_id = @cust_aurora);

-- ---------------------------------------------------------------------------
-- 4. ADDITIONAL PROGRAMS (variance scenarios)
-- ---------------------------------------------------------------------------
SET @cust_aurora_vp  := IFNULL((SELECT customer_id FROM platform_customer WHERE customer_code = 'AURORA' LIMIT 1),
                               (SELECT customer_id FROM platform_customer WHERE customer_code = 'DEFAULT' LIMIT 1));
SET @cust_horizon_vp := IFNULL((SELECT customer_id FROM platform_customer WHERE customer_code = 'HORIZON' LIMIT 1),
                               (SELECT customer_id FROM platform_customer WHERE customer_code = 'DEFAULT' LIMIT 1));

INSERT INTO vehicle_program (
    program_id, customer_id, program_code, model_name, platform_code, sourcing_region,
    lifetime_volume, target_cost, target_currency, status, created_at
)
SELECT 'a2000000-0000-4000-8000-000000000011', @cust_aurora_vp, 'EV-SUV-2028', 'Aurora EV SUV Platform',
       'PLT-SUV-A', 'NA+APAC', 450000, 1850.00, 'USD', 'Active', UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM vehicle_program WHERE program_code = 'EV-SUV-2028');

INSERT INTO vehicle_program (
    program_id, customer_id, program_code, model_name, platform_code, sourcing_region,
    lifetime_volume, target_cost, target_currency, status, created_at
)
SELECT 'a2000000-0000-4000-8000-000000000012', @cust_horizon_vp, 'ICE-SED-2026', 'Horizon ICE Sedan',
       'PLT-SED-H', 'APAC', 220000, 920.00, 'INR', 'Active', UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM vehicle_program WHERE program_code = 'ICE-SED-2026');

INSERT INTO vehicle_program (
    program_id, customer_id, program_code, model_name, platform_code, sourcing_region,
    lifetime_volume, target_cost, target_currency, status, created_at
)
SELECT 'a2000000-0000-4000-8000-000000000013', @cust_aurora_vp, 'HYB-CUV-2029', 'Aurora Hybrid CUV',
       'PLT-CUV-B', 'NA', 180000, 2100.00, 'USD', 'Active', UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM vehicle_program WHERE program_code = 'HYB-CUV-2029');

-- ---------------------------------------------------------------------------
-- 5. ADDITIONAL SUPPLIERS
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO supplier_master (
    supplier_id, supplier_code, supplier_name, country_code, status, created_at
)
SELECT 'c1000000-0000-4000-8000-000000000011', 'STEELCO', 'SteelCo North America',
       'US', 'ACTIVE', UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM supplier_master WHERE supplier_code = 'STEELCO');

INSERT IGNORE INTO supplier_master (
    supplier_id, supplier_code, supplier_name, country_code, status, created_at
)
SELECT 'c1000000-0000-4000-8000-000000000012', 'POLYTECH', 'PolyTech Polymers Pvt Ltd',
       'IN', 'ACTIVE', UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM supplier_master WHERE supplier_code = 'POLYTECH');

INSERT IGNORE INTO supplier_master (
    supplier_id, supplier_code, supplier_name, country_code, status, created_at
)
SELECT 'c1000000-0000-4000-8000-000000000013', 'PRECISION-CN', 'Precision Components China',
       'CN', 'UNDER_VERIFICATION', UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM supplier_master WHERE supplier_code = 'PRECISION-CN');

INSERT IGNORE INTO supplier_master (
    supplier_id, supplier_code, supplier_name, country_code, status, created_at
)
SELECT 'c1000000-0000-4000-8000-000000000021', 'HOR-SUP-01', 'Horizon Local Stamping',
       'IN', 'ACTIVE', UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM supplier_master WHERE supplier_code = 'HOR-SUP-01');

-- ---------------------------------------------------------------------------
-- 6. ADDITIONAL BOM / PRODUCTS (multi-level, MAKE/BUY, incomplete)
-- Schema: bom_node requires customer_id; no description column
-- ---------------------------------------------------------------------------
SET @cust_aurora  := IFNULL((SELECT customer_id FROM platform_customer WHERE customer_code = 'AURORA' LIMIT 1),
                            (SELECT customer_id FROM platform_customer WHERE customer_code = 'DEFAULT' LIMIT 1));
SET @cust_horizon := IFNULL((SELECT customer_id FROM platform_customer WHERE customer_code = 'HORIZON' LIMIT 1),
                            (SELECT customer_id FROM platform_customer WHERE customer_code = 'DEFAULT' LIMIT 1));

INSERT INTO bom_node (
    bom_node_id, customer_id, program_id, parent_node_id, part_number, revision,
    make_buy_flag, quantity_per, uom, status, what_if_branch_flag, override_flag, created_at
)
SELECT 'b1000000-0000-4000-8000-000000000011', @cust_aurora,
       'a2000000-0000-4000-8000-000000000011',
       NULL, 'SUV-ROOT', 'A',
       'MAKE', 1.000000, 'EA', 'RELEASED', FALSE, FALSE, UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM bom_node WHERE bom_node_id = 'b1000000-0000-4000-8000-000000000011');

INSERT INTO bom_node (
    bom_node_id, customer_id, program_id, parent_node_id, part_number, revision,
    make_buy_flag, quantity_per, uom, status, material_code, net_weight, scrap_pct,
    what_if_branch_flag, override_flag, created_at
)
SELECT 'b1000000-0000-4000-8000-000000000012', @cust_aurora,
       'a2000000-0000-4000-8000-000000000011',
       'b1000000-0000-4000-8000-000000000011',
       'BRKT-SUV-FR', 'A',
       'MAKE', 2.000000, 'EA', 'RELEASED', 'HSLA-550', 1.850000, 0.040000,
       FALSE, FALSE, UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM bom_node WHERE bom_node_id = 'b1000000-0000-4000-8000-000000000012');

INSERT INTO bom_node (
    bom_node_id, customer_id, program_id, parent_node_id, part_number, revision,
    make_buy_flag, quantity_per, uom, status, what_if_branch_flag, override_flag, created_at
)
SELECT 'b1000000-0000-4000-8000-000000000013', @cust_aurora,
       'a2000000-0000-4000-8000-000000000011',
       'b1000000-0000-4000-8000-000000000011',
       'HSG-MOTOR-01', 'B',
       'BUY', 1.000000, 'EA', 'RELEASED', FALSE, FALSE, UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM bom_node WHERE bom_node_id = 'b1000000-0000-4000-8000-000000000013');

INSERT INTO bom_node (
    bom_node_id, customer_id, program_id, parent_node_id, part_number, revision,
    make_buy_flag, quantity_per, uom, status, what_if_branch_flag, override_flag, created_at
)
SELECT 'b1000000-0000-4000-8000-000000000014', @cust_aurora,
       'a2000000-0000-4000-8000-000000000011',
       'b1000000-0000-4000-8000-000000000011',
       'CLIP-INCOMPLETE', 'A',
       'MAKE', 4.000000, 'EA', 'DRAFT', FALSE, FALSE, UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM bom_node WHERE bom_node_id = 'b1000000-0000-4000-8000-000000000014');

INSERT INTO bom_node (
    bom_node_id, customer_id, program_id, parent_node_id, part_number, revision,
    make_buy_flag, quantity_per, uom, status, what_if_branch_flag, override_flag, created_at
)
SELECT 'b1000000-0000-4000-8000-000000000021', @cust_horizon,
       'a2000000-0000-4000-8000-000000000012',
       NULL, 'SED-ROOT', 'A',
       'MAKE', 1.000000, 'EA', 'RELEASED', FALSE, FALSE, UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM bom_node WHERE bom_node_id = 'b1000000-0000-4000-8000-000000000021');

INSERT INTO bom_node (
    bom_node_id, customer_id, program_id, parent_node_id, part_number, revision,
    make_buy_flag, quantity_per, uom, status, material_code, net_weight, scrap_pct,
    what_if_branch_flag, override_flag, created_at
)
SELECT 'b1000000-0000-4000-8000-000000000022', @cust_horizon,
       'a2000000-0000-4000-8000-000000000012',
       'b1000000-0000-4000-8000-000000000021',
       'PANEL-DOOR-LH', 'A',
       'MAKE', 1.000000, 'EA', 'RELEASED', 'CRS-1010', 3.200000, 0.060000,
       FALSE, FALSE, UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM bom_node WHERE bom_node_id = 'b1000000-0000-4000-8000-000000000022');

-- ---------------------------------------------------------------------------
-- 7. SHOULD-COST / TARGET (variance scenarios: below / near / above)
-- ---------------------------------------------------------------------------
-- Bracket should-cost (near target)
INSERT IGNORE INTO should_cost_build_up (
    build_up_id, bom_node_id, scenario_label,
    material_cost, process_cost, tooling_cost, packaging_freight_cost,
    overhead_cost, sga_cost, profit_margin, total_cost,
    stale_inputs_flag, locked, calculation_basis, calculated_at, approval_status, created_at
)
VALUES (
    'e1000000-0000-4000-8000-000000000011',
    'b1000000-0000-4000-8000-000000000012',
    'Baseline HSLA bracket',
    1.850000, 1.200000, 0.150000, 0.320000,
    0.480000, 0.220000, 0.180000, 4.400000,
    FALSE, FALSE,
    'Demo build-up BRKT-SUV-FR',
    UTC_TIMESTAMP(6), 'IN_REVIEW', UTC_TIMESTAMP(6)
);

INSERT IGNORE INTO target_cost_record (
    target_cost_id, bom_node_id, program_id, target_amount, currency_code,
    effective_from, status, approval_status, notes, created_at
)
VALUES (
    'e1000000-0000-4000-8000-000000000012',
    'b1000000-0000-4000-8000-000000000012',
    'a2000000-0000-4000-8000-000000000011',
    4.250000, 'USD',
    CURRENT_DATE, 'ACTIVE', 'APPROVED',
    'Near target â€” small favorable variance', UTC_TIMESTAMP(6)
);

-- Housing BUY should-cost (above target â€” variance)
INSERT IGNORE INTO should_cost_build_up (
    build_up_id, bom_node_id, scenario_label,
    material_cost, process_cost, tooling_cost, packaging_freight_cost,
    overhead_cost, sga_cost, profit_margin, total_cost,
    stale_inputs_flag, locked, calculation_basis, calculated_at, approval_status, created_at
)
VALUES (
    'e1000000-0000-4000-8000-000000000013',
    'b1000000-0000-4000-8000-000000000013',
    'Buy housing parametric',
    0, 0, 0, 0.800000,
    0, 0.400000, 0.500000, 18.500000,
    FALSE, FALSE,
    'Parametric BUY housing',
    UTC_TIMESTAMP(6), 'PENDING_APPROVAL', UTC_TIMESTAMP(6)
);

INSERT IGNORE INTO target_cost_record (
    target_cost_id, bom_node_id, program_id, target_amount, currency_code,
    effective_from, status, approval_status, notes, created_at
)
VALUES (
    'e1000000-0000-4000-8000-000000000014',
    'b1000000-0000-4000-8000-000000000013',
    'a2000000-0000-4000-8000-000000000011',
    15.000000, 'USD',
    CURRENT_DATE, 'ACTIVE', 'APPROVED',
    'Above target â€” negotiation required', UTC_TIMESTAMP(6)
);

-- Released costing (manager completed work)
INSERT IGNORE INTO should_cost_build_up (
    build_up_id, bom_node_id, scenario_label,
    material_cost, process_cost, tooling_cost, packaging_freight_cost,
    overhead_cost, sga_cost, profit_margin, total_cost,
    stale_inputs_flag, locked, calculation_basis, calculated_at, approval_status, created_at
)
VALUES (
    'e1000000-0000-4000-8000-000000000015',
    'b1000000-0000-4000-8000-000000000002',
    'Released CA-LH-FR v2',
    2.370000, 1.850000, 0.420000, 0.770000,
    1.100000, 0.450000, 0.350000, 7.310000,
    FALSE, TRUE,
    'Released baseline',
    UTC_TIMESTAMP(6), 'RELEASED', UTC_TIMESTAMP(6)
);

-- Rejected costing
INSERT IGNORE INTO should_cost_build_up (
    build_up_id, bom_node_id, scenario_label,
    material_cost, process_cost, tooling_cost, packaging_freight_cost,
    overhead_cost, sga_cost, profit_margin, total_cost,
    stale_inputs_flag, locked, calculation_basis, calculated_at, approval_status, created_at
)
VALUES (
    'e1000000-0000-4000-8000-000000000016',
    'b1000000-0000-4000-8000-000000000014',
    'Rejected incomplete clip',
    0.100000, 0.050000, 0, 0.020000,
    0.030000, 0.010000, 0.010000, 0.220000,
    TRUE, FALSE,
    'Rejected â€” incomplete inputs',
    UTC_TIMESTAMP(6), 'REJECTED', UTC_TIMESTAMP(6)
);

-- ---------------------------------------------------------------------------
-- 8. RFQs + QUOTATIONS (multiple suppliers, competing)
-- ---------------------------------------------------------------------------
-- Schema-aligned RFQ demo data.
-- rfq_event is the authoritative RFQ parent table (V16).
-- Valid rfq_event statuses: DRAFT, OPEN, CLOSED, CANCELLED, AWARDED.

INSERT IGNORE INTO rfq_event (
    rfq_id, rfq_number, program_id, title, status, currency_code,
    due_date, created_by, created_at, notes
)
SELECT
    'd1000000-0000-4000-8000-000000000011',
    'RFQ-AUR-2026-001',
    'a2000000-0000-4000-8000-000000000011',
    'Motor housing supplier RFQ',
    'OPEN',
    'USD',
    DATE_ADD(CURRENT_DATE, INTERVAL 7 DAY),
    'f1000000-0000-4000-8000-000000000001',
    UTC_TIMESTAMP(6),
    'Demo stage: evaluation'
WHERE NOT EXISTS (
    SELECT 1 FROM rfq_event
    WHERE rfq_id = 'd1000000-0000-4000-8000-000000000011'
);

INSERT IGNORE INTO rfq_event (
    rfq_id, rfq_number, program_id, title, status, currency_code,
    due_date, created_by, created_at, notes
)
SELECT
    'd1000000-0000-4000-8000-000000000012',
    'RFQ-AUR-2026-002',
    'a2000000-0000-4000-8000-000000000011',
    'Motor housing open RFQ',
    'OPEN',
    'USD',
    DATE_ADD(CURRENT_DATE, INTERVAL 3 DAY),
    'f1000000-0000-4000-8000-000000000003',
    UTC_TIMESTAMP(6),
    'Demo stage: awaiting response'
WHERE NOT EXISTS (
    SELECT 1 FROM rfq_event
    WHERE rfq_id = 'd1000000-0000-4000-8000-000000000012'
);

INSERT IGNORE INTO rfq_event (
    rfq_id, rfq_number, program_id, title, status, currency_code,
    due_date, created_by, created_at, notes
)
SELECT
    'd1000000-0000-4000-8000-000000000013',
    'RFQ-AUR-2026-003',
    'a2000000-0000-4000-8000-000000000013',
    'Hybrid CUV supplier RFQ',
    'OPEN',
    'USD',
    DATE_ADD(CURRENT_DATE, INTERVAL 14 DAY),
    'f1000000-0000-4000-8000-000000000007',
    UTC_TIMESTAMP(6),
    'Demo stage: quotation'
WHERE NOT EXISTS (
    SELECT 1 FROM rfq_event
    WHERE rfq_id = 'd1000000-0000-4000-8000-000000000013'
);

INSERT IGNORE INTO rfq_event (
    rfq_id, rfq_number, program_id, title, status, currency_code,
    due_date, created_by, created_at, notes
)
SELECT
    'd1000000-0000-4000-8000-000000000014',
    'RFQ-AUR-2026-004',
    'a2000000-0000-4000-8000-000000000011',
    'Awarded wheel bearing RFQ',
    'AWARDED',
    'USD',
    DATE_SUB(CURRENT_DATE, INTERVAL 5 DAY),
    'f1000000-0000-4000-8000-000000000003',
    UTC_TIMESTAMP(6),
    'Demo stage: awarded'
WHERE NOT EXISTS (
    SELECT 1 FROM rfq_event
    WHERE rfq_id = 'd1000000-0000-4000-8000-000000000014'
);

INSERT IGNORE INTO rfq_event (
    rfq_id, rfq_number, program_id, title, status, currency_code,
    due_date, created_by, created_at, notes
)
SELECT
    'd1000000-0000-4000-8000-000000000015',
    'RFQ-AUR-2026-005',
    'a2000000-0000-4000-8000-000000000001',
    'Closed historical RFQ',
    'CLOSED',
    'USD',
    DATE_SUB(CURRENT_DATE, INTERVAL 30 DAY),
    'f1000000-0000-4000-8000-000000000003',
    UTC_TIMESTAMP(6),
    'Demo stage: closed'
WHERE NOT EXISTS (
    SELECT 1 FROM rfq_event
    WHERE rfq_id = 'd1000000-0000-4000-8000-000000000015'
);

-- ---------------------------------------------------------------------------
-- 9. PURCHASE REQUESTS + PURCHASE ORDERS
-- ---------------------------------------------------------------------------
-- PR linked to existing BUY bearing (V18)
INSERT IGNORE INTO purchase_request (
    purchase_request_id, customer_id, program_id, bom_node_id,
    status, reporting_currency, notes, created_by, created_at
)
SELECT 'p1000000-0000-4000-8000-000000000001',
       (SELECT customer_id FROM platform_customer WHERE customer_code = 'DEFAULT' LIMIT 1),
       'a2000000-0000-4000-8000-000000000001',
       'b1000000-0000-4000-8000-000000000003',
       'RELEASED', 'USD',
       'PR for HUB-BRG-FR from awarded RFQ',
       'f1000000-0000-4000-8000-000000000003', UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM purchase_request WHERE purchase_request_id = 'p1000000-0000-4000-8000-000000000001');

INSERT IGNORE INTO purchase_request (
    purchase_request_id, customer_id, program_id, bom_node_id,
    status, reporting_currency, notes, created_by, created_at
)
SELECT 'p1000000-0000-4000-8000-000000000002',
       (SELECT customer_id FROM platform_customer WHERE customer_code = 'DEFAULT' LIMIT 1),
       'a2000000-0000-4000-8000-000000000011',
       'b1000000-0000-4000-8000-000000000013',
       'APPROVAL', 'USD',
       'PR motor housing â€” pending manager approval',
       'f1000000-0000-4000-8000-000000000021', UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM purchase_request WHERE purchase_request_id = 'p1000000-0000-4000-8000-000000000002');

INSERT IGNORE INTO purchase_request (
    purchase_request_id, customer_id, program_id, bom_node_id,
    status, reporting_currency, notes, created_by, created_at
)
SELECT 'p1000000-0000-4000-8000-000000000003',
       (SELECT customer_id FROM platform_customer WHERE customer_code = 'DEFAULT' LIMIT 1),
       'a2000000-0000-4000-8000-000000000011',
       'b1000000-0000-4000-8000-000000000012',
       'DRAFT', 'USD',
       'Draft PR bracket â€” incomplete',
       'f1000000-0000-4000-8000-000000000022', UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM purchase_request WHERE purchase_request_id = 'p1000000-0000-4000-8000-000000000003');

-- POs
INSERT IGNORE INTO purchase_order (
    purchase_order_id, po_number, customer_id, purchase_request_id, linked_rfq_id,
    supplier_id, plant_code, buyer_user_id, currency_code,
    payment_terms, order_date, required_delivery_date, status,
    notes, created_by, created_at
)
SELECT 'p1000000-0000-4000-8000-000000000011', 'PO-AUR-2026-001',
       (SELECT customer_id FROM platform_customer WHERE customer_code = 'DEFAULT' LIMIT 1),
       'p1000000-0000-4000-8000-000000000001',
       'd1000000-0000-4000-8000-000000000001',
       'c1000000-0000-4000-8000-000000000001',
       'DET-01', 'f1000000-0000-4000-8000-000000000003', 'USD',
       'Net 45', CURRENT_DATE, DATE_ADD(CURRENT_DATE, INTERVAL 30 DAY), 'ISSUED',
       'Issued PO for wheel bearing', 'f1000000-0000-4000-8000-000000000003', UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM purchase_order WHERE purchase_order_id = 'p1000000-0000-4000-8000-000000000011');

INSERT IGNORE INTO purchase_order (
    purchase_order_id, po_number, customer_id, purchase_request_id,
    supplier_id, plant_code, buyer_user_id, currency_code,
    payment_terms, order_date, required_delivery_date, status,
    notes, created_by, created_at
)
SELECT 'p1000000-0000-4000-8000-000000000012', 'PO-AUR-2026-002',
       (SELECT customer_id FROM platform_customer WHERE customer_code = 'DEFAULT' LIMIT 1),
       NULL,
       'c1000000-0000-4000-8000-000000000011',
       'AUR-DET', 'f1000000-0000-4000-8000-000000000021', 'USD',
       'Net 30', DATE_SUB(CURRENT_DATE, INTERVAL 45 DAY),
       DATE_SUB(CURRENT_DATE, INTERVAL 10 DAY), 'ISSUED',
       'OVERDUE â€” delivery past due', 'f1000000-0000-4000-8000-000000000021', UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM purchase_order WHERE purchase_order_id = 'p1000000-0000-4000-8000-000000000012');

INSERT IGNORE INTO purchase_order (
    purchase_order_id, po_number, customer_id, purchase_request_id,
    supplier_id, plant_code, buyer_user_id, currency_code,
    payment_terms, order_date, required_delivery_date, status,
    notes, created_by, created_at
)
SELECT 'p1000000-0000-4000-8000-000000000013', 'PO-AUR-2026-003',
       (SELECT customer_id FROM platform_customer WHERE customer_code = 'DEFAULT' LIMIT 1),
       'p1000000-0000-4000-8000-000000000002',
       'c1000000-0000-4000-8000-000000000002',
       'AUR-PUN', 'f1000000-0000-4000-8000-000000000003', 'USD',
       'Net 60', CURRENT_DATE, DATE_ADD(CURRENT_DATE, INTERVAL 60 DAY), 'APPROVAL',
       'Pending purchasing manager approval', 'f1000000-0000-4000-8000-000000000003', UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM purchase_order WHERE purchase_order_id = 'p1000000-0000-4000-8000-000000000013');

INSERT IGNORE INTO purchase_order (
    purchase_order_id, po_number, customer_id, purchase_request_id,
    supplier_id, plant_code, buyer_user_id, currency_code,
    payment_terms, order_date, required_delivery_date, status,
    notes, created_by, created_at
)
SELECT 'p1000000-0000-4000-8000-000000000014', 'PO-AUR-2026-004',
       (SELECT customer_id FROM platform_customer WHERE customer_code = 'DEFAULT' LIMIT 1),
       NULL,
       'c1000000-0000-4000-8000-000000000012',
       'AUR-DET', 'f1000000-0000-4000-8000-000000000022', 'USD',
       'Net 30', DATE_SUB(CURRENT_DATE, INTERVAL 90 DAY),
       DATE_SUB(CURRENT_DATE, INTERVAL 60 DAY), 'CLOSED',
       'Closed historical PO', 'f1000000-0000-4000-8000-000000000022', UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM purchase_order WHERE purchase_order_id = 'p1000000-0000-4000-8000-000000000014');

-- PO lines (where table exists)
INSERT IGNORE INTO purchase_order_line (
    line_id, purchase_order_id, line_no, material_code, material_description,
    quantity, uom, unit_price, currency_code, delivery_date, created_at
)
SELECT 'p1000000-0000-4000-8000-000000000111',
       'p1000000-0000-4000-8000-000000000011', 1,
       'HUB-BRG-FR', 'Front hub bearing',
       5000.000000, 'EA', 12.450000, 'USD',
       DATE_ADD(CURRENT_DATE, INTERVAL 30 DAY), UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM purchase_order_line WHERE line_id = 'p1000000-0000-4000-8000-000000000111');

INSERT IGNORE INTO purchase_order_line (
    line_id, purchase_order_id, line_no, material_code, material_description,
    quantity, uom, unit_price, currency_code, delivery_date, created_at
)
SELECT 'p1000000-0000-4000-8000-000000000112',
       'p1000000-0000-4000-8000-000000000012', 1,
       'HSLA-550', 'HSLA steel coil',
       12000.000000, 'KG', 0.980000, 'USD',
       DATE_SUB(CURRENT_DATE, INTERVAL 10 DAY), UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM purchase_order_line WHERE line_id = 'p1000000-0000-4000-8000-000000000112');

-- ---------------------------------------------------------------------------
-- 10. TOOLING (tooling_record schema: tooling_record_id, bom_node_id, tooling_type, ...)
-- ---------------------------------------------------------------------------
INSERT INTO tooling_record (
    tooling_record_id, bom_node_id, customer_id, tooling_type, description, cavity_count,
    tool_cost, tool_life_hits, ownership, supplier_id, asset_status, created_at
)
SELECT 't1000000-0000-4000-8000-000000000001',
       'b1000000-0000-4000-8000-000000000002',
       (SELECT customer_id FROM bom_node WHERE bom_node_id = 'b1000000-0000-4000-8000-000000000002' LIMIT 1),
       'DIE', 'Control arm LH progressive die', 1,
       185000.000000, 500000.00, 'CUSTOMER_OWNED',
       'c1000000-0000-4000-8000-000000000001', 'ACTIVE', UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM tooling_record WHERE tooling_record_id = 't1000000-0000-4000-8000-000000000001')
  AND EXISTS (SELECT 1 FROM bom_node WHERE bom_node_id = 'b1000000-0000-4000-8000-000000000002');

INSERT INTO tooling_record (
    tooling_record_id, bom_node_id, customer_id, tooling_type, description, cavity_count,
    tool_cost, tool_life_hits, ownership, asset_status, created_at
)
SELECT 't1000000-0000-4000-8000-000000000002',
       'b1000000-0000-4000-8000-000000000012',
       (SELECT customer_id FROM bom_node WHERE bom_node_id = 'b1000000-0000-4000-8000-000000000012' LIMIT 1),
       'FIXTURE', 'Bracket welding fixture', 1,
       42000.000000, 200000.00, 'CUSTOMER_OWNED', 'PENDING', UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM tooling_record WHERE tooling_record_id = 't1000000-0000-4000-8000-000000000002')
  AND EXISTS (SELECT 1 FROM bom_node WHERE bom_node_id = 'b1000000-0000-4000-8000-000000000012');

INSERT INTO tooling_record (
    tooling_record_id, bom_node_id, customer_id, tooling_type, description, cavity_count,
    tool_cost, tool_life_hits, ownership, asset_status, created_at
)
SELECT 't1000000-0000-4000-8000-000000000003',
       'b1000000-0000-4000-8000-000000000013',
       (SELECT customer_id FROM bom_node WHERE bom_node_id = 'b1000000-0000-4000-8000-000000000013' LIMIT 1),
       'GAUGE', 'Housing CMM gauge set', 1,
       18500.000000, NULL, 'CUSTOMER_OWNED', 'ACTIVE', UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM tooling_record WHERE tooling_record_id = 't1000000-0000-4000-8000-000000000003')
  AND EXISTS (SELECT 1 FROM bom_node WHERE bom_node_id = 'b1000000-0000-4000-8000-000000000013');

INSERT INTO tooling_record (
    tooling_record_id, bom_node_id, customer_id, tooling_type, description, cavity_count,
    tool_cost, tool_life_hits, ownership, asset_status, created_at
)
SELECT 't1000000-0000-4000-8000-000000000004',
       'b1000000-0000-4000-8000-000000000014',
       (SELECT customer_id FROM bom_node WHERE bom_node_id = 'b1000000-0000-4000-8000-000000000014' LIMIT 1),
       'MOLD', 'Clip injection mold (high value)', 4,
       275000.000000, 1000000.00, 'SUPPLIER_OWNED', 'PENDING', UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM tooling_record WHERE tooling_record_id = 't1000000-0000-4000-8000-000000000004')
  AND EXISTS (SELECT 1 FROM bom_node WHERE bom_node_id = 'b1000000-0000-4000-8000-000000000014');

-- ---------------------------------------------------------------------------
-- 11. FX RATES (Finance Controller)
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO exchange_rate (
    rate_id, from_currency, to_currency, rate, effective_date, source, created_at
)
SELECT UUID(), 'USD', 'INR', 83.25000000, CURRENT_DATE, 'DEMO_SEED', UTC_TIMESTAMP(6)
WHERE NOT EXISTS (
    SELECT 1 FROM exchange_rate
    WHERE from_currency = 'USD' AND to_currency = 'INR' AND effective_date = CURRENT_DATE
);

INSERT IGNORE INTO exchange_rate (
    rate_id, from_currency, to_currency, rate, effective_date, source, created_at
)
SELECT UUID(), 'EUR', 'USD', 1.08500000, CURRENT_DATE, 'DEMO_SEED', UTC_TIMESTAMP(6)
WHERE NOT EXISTS (
    SELECT 1 FROM exchange_rate
    WHERE from_currency = 'EUR' AND to_currency = 'USD' AND effective_date = CURRENT_DATE
);

INSERT IGNORE INTO exchange_rate (
    rate_id, from_currency, to_currency, rate, effective_date, source, created_at
)
SELECT UUID(), 'USD', 'EUR', 0.92170000, CURRENT_DATE, 'DEMO_SEED', UTC_TIMESTAMP(6)
WHERE NOT EXISTS (
    SELECT 1 FROM exchange_rate
    WHERE from_currency = 'USD' AND to_currency = 'EUR' AND effective_date = CURRENT_DATE
);

INSERT IGNORE INTO exchange_rate (
    rate_id, from_currency, to_currency, rate, effective_date, source, created_at
)
SELECT UUID(), 'USD', 'INR', 82.90000000, DATE_SUB(CURRENT_DATE, INTERVAL 30 DAY), 'DEMO_SEED', UTC_TIMESTAMP(6)
WHERE NOT EXISTS (
    SELECT 1 FROM exchange_rate
    WHERE from_currency = 'USD' AND to_currency = 'INR'
      AND effective_date = DATE_SUB(CURRENT_DATE, INTERVAL 30 DAY)
);

-- ---------------------------------------------------------------------------
-- 12. SUPPORT TICKETS (multi-tenant, multi-state)
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO support_ticket (
    ticket_id, ticket_number, customer_id, created_by, subject, description,
    category, priority, status, assigned_to, created_at
)
SELECT 's1000000-0000-4000-8000-000000000001', 'TKT-10001',
       (SELECT customer_id FROM platform_customer WHERE customer_code = 'DEFAULT' LIMIT 1),
       'f1000000-0000-4000-8000-000000000001',
       'Should-cost variance unexplained',
       'Control arm should-cost vs quote variance needs review',
       'Costing', 'HIGH', 'OPEN',
       'f1000000-0000-4000-8000-000000000002', UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM support_ticket WHERE ticket_number = 'TKT-10001');

INSERT IGNORE INTO support_ticket (
    ticket_id, ticket_number, customer_id, created_by, subject, description,
    category, priority, status, assigned_to, created_at
)
SELECT 's1000000-0000-4000-8000-000000000002', 'TKT-10002',
       (SELECT customer_id FROM platform_customer WHERE customer_code = 'DEFAULT' LIMIT 1),
       'f1000000-0000-4000-8000-000000000003',
       'RFQ attachment upload failure',
       'Cannot attach supplier drawings to RFQ-AUR-2026-002',
       'RFQ', 'MEDIUM', 'IN_PROGRESS',
       'f1000000-0000-4000-8000-000000000005', UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM support_ticket WHERE ticket_number = 'TKT-10002');

INSERT IGNORE INTO support_ticket (
    ticket_id, ticket_number, customer_id, created_by, subject, description,
    category, priority, status, created_at, resolved_at
)
SELECT 's1000000-0000-4000-8000-000000000003', 'TKT-10003',
       (SELECT customer_id FROM platform_customer WHERE customer_code = 'DEFAULT' LIMIT 1),
       'f1000000-0000-4000-8000-000000000004',
       'Material master incomplete warning',
       'INCOMPLETE-MAT-01 appears on dashboard attention list',
       'Data Issue', 'LOW', 'RESOLVED',
       DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 5 DAY),
       DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 2 DAY)
WHERE NOT EXISTS (SELECT 1 FROM support_ticket WHERE ticket_number = 'TKT-10003');

INSERT IGNORE INTO support_ticket (
    ticket_id, ticket_number, customer_id, created_by, subject, description,
    category, priority, status, assigned_to, created_at
)
SELECT 's1000000-0000-4000-8000-000000000004', 'TKT-10004',
       (SELECT customer_id FROM platform_customer WHERE customer_code = 'DEFAULT' LIMIT 1),
       'f1000000-0000-4000-8000-000000000001',
       'Login MFA intermittent failure',
       'MFA challenge times out for some users',
       'Login/Access', 'CRITICAL', 'WAITING_FOR_CUSTOMER',
       'f1000000-0000-4000-8000-000000000005', UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM support_ticket WHERE ticket_number = 'TKT-10004');

INSERT IGNORE INTO support_ticket (
    ticket_id, ticket_number, customer_id, created_by, subject, description,
    category, priority, status, created_at, closed_at
)
SELECT 's1000000-0000-4000-8000-000000000005', 'TKT-10005',
       (SELECT customer_id FROM platform_customer WHERE customer_code = 'DEFAULT' LIMIT 1),
       'f1000000-0000-4000-8000-000000000008',
       'BOM import column mapping',
       'ERP BOM promote mapping documented and closed',
       'BOM', 'MEDIUM', 'CLOSED',
       DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 20 DAY),
       DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 15 DAY)
WHERE NOT EXISTS (SELECT 1 FROM support_ticket WHERE ticket_number = 'TKT-10005');

-- Horizon tickets (must not leak to Aurora/DEFAULT)
INSERT IGNORE INTO support_ticket (
    ticket_id, ticket_number, customer_id, created_by, subject, description,
    category, priority, status, created_at
)
SELECT 's1000000-0000-4000-8000-000000000021', 'TKT-H-2001',
       (SELECT customer_id FROM platform_customer WHERE customer_code = 'HORIZON' LIMIT 1),
       'f1000000-0000-4000-8000-000000000032',
       'Horizon program target cost setup',
       'Need help setting ICE-SED-2026 target costs',
       'Costing', 'HIGH', 'OPEN', UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM support_ticket WHERE ticket_number = 'TKT-H-2001');

INSERT IGNORE INTO support_ticket (
    ticket_id, ticket_number, customer_id, created_by, subject, description,
    category, priority, status, created_at
)
SELECT 's1000000-0000-4000-8000-000000000022', 'TKT-H-2002',
       (SELECT customer_id FROM platform_customer WHERE customer_code = 'HORIZON' LIMIT 1),
       'f1000000-0000-4000-8000-000000000033',
       'Purchase module access for buyer',
       'Buyer role cannot see PR list',
       'Configuration', 'MEDIUM', 'IN_PROGRESS', UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM support_ticket WHERE ticket_number = 'TKT-H-2002');

-- Ticket messages
INSERT IGNORE INTO support_ticket_message (
    message_id, ticket_id, author_user_id, author_type, message_body, created_at
)
SELECT 's1000000-0000-4000-8000-000000000101',
       's1000000-0000-4000-8000-000000000001',
       'f1000000-0000-4000-8000-000000000001', 'CUSTOMER',
       'Please review material rate assumptions on CRS-1010.',
       UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM support_ticket_message WHERE message_id = 's1000000-0000-4000-8000-000000000101');

INSERT IGNORE INTO support_ticket_message (
    message_id, ticket_id, author_user_id, author_type, message_body, created_at
)
SELECT 's1000000-0000-4000-8000-000000000102',
       's1000000-0000-4000-8000-000000000001',
       'f1000000-0000-4000-8000-000000000002', 'SUPPORT',
       'Acknowledged. Comparing should-cost build-up e100â€¦001 against ACME quote.',
       UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM support_ticket_message WHERE message_id = 's1000000-0000-4000-8000-000000000102');

INSERT IGNORE INTO support_ticket_internal_note (
    note_id, ticket_id, author_user_id, note_body, created_at
)
SELECT 's1000000-0000-4000-8000-000000000201',
       's1000000-0000-4000-8000-000000000001',
       'f1000000-0000-4000-8000-000000000005',
       'Internal: possible scrap % mismatch â€” do not share with customer yet.',
       UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM support_ticket_internal_note WHERE note_id = 's1000000-0000-4000-8000-000000000201');

-- -- ---------------------------------------------------------------------------
-- -- ---------------------------------------------------------------------------
-- -- 13. AUDIT / ACTIVITY (platform + customer)
-- -- ---------------------------------------------------------------------------
-- INSERT INTO audit_log (
--     audit_id,
--     entity_type,
--     entity_id,
--     action,
--     details,
--     created_at
-- )
-- SELECT
--     UUID(),
--     'SHOULD_COST',
--     'e1000000-0000-4000-8000-000000000001',
--     'STATUS_CHANGE',
--     'Demo: baseline moved to review',
--     UTC_TIMESTAMP(6)
-- WHERE NOT EXISTS (
--     SELECT 1
--     FROM audit_log
--     WHERE entity_type = 'SHOULD_COST'
--       AND entity_id = 'e1000000-0000-4000-8000-000000000001'
--       AND action = 'STATUS_CHANGE'
-- );
-- INSERT INTO audit_log (
--     audit_id,
--     entity_type,
--     entity_id,
--     action,
--     details,
--     created_at
-- )
-- SELECT
--     UUID(),
--     'PURCHASE_ORDER',
--     'p1000000-0000-4000-8000-000000000011',
--     'ISSUED',
--     'Demo: PO-AUR-2026-001 issued',
--     UTC_TIMESTAMP(6)
-- WHERE NOT EXISTS (
--     SELECT 1
--     FROM audit_log
--     WHERE entity_type = 'PURCHASE_ORDER'
--       AND entity_id = 'p1000000-0000-4000-8000-000000000011'
--       AND action = 'ISSUED'
-- );
-- INSERT INTO audit_log (
--     audit_id,
--     entity_type,
--     entity_id,
--     action,
--     details,
--     created_at
-- )
-- SELECT
--     UUID(),
--     'SUPPORT_TICKET',
--     'T1000000-0000-4000-8000-000000000001',
--     'CREATED',
--     'Demo ticket opened',
--     UTC_TIMESTAMP(6)
-- WHERE NOT EXISTS (
--     SELECT 1
--     FROM audit_log
--     WHERE entity_type = 'SUPPORT_TICKET'
--       AND entity_id = 'T1000000-0000-4000-8000-000000000001'
--       AND action = 'CREATED'
-- );
-- -- ---------------------------------------------------------------------------
-- -- END OF V167 DEMO DATA
-- -- ---------------------------------------------------------------------------

