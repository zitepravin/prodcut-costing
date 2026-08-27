-- =============================================================================
-- V18 Demo seed — realistic automotive data for every major function
-- Password for all demo users: ChangeMe!Now1
-- BCrypt (same as V9 admin): $2b$10$TWZ.5vAxuEf/dDNqxblzr.JrLtk2NG0I63008RTvJbZOnGkEIV0pe
-- Idempotent where possible: fixed UUIDs; INSERT IGNORE / not exists patterns
-- =============================================================================

-- ---------- Fixed IDs (documentation / DEMO_WALKTHROUGH) ----------
-- Plant DET-01          a1000000-0000-4000-8000-000000000001
-- Plant PUN-01          a1000000-0000-4000-8000-000000000002
-- Program EV-CX-2027     a2000000-0000-4000-8000-000000000001
-- Material CRS-1010      a3000000-0000-4000-8000-000000000001
-- Material AL-6061       a3000000-0000-4000-8000-000000000002
-- Machine PRESS-800T     a4000000-0000-4000-8000-000000000001
-- Labor OP-STAMP         a5000000-0000-4000-8000-000000000001
-- Overhead OH-MFG        a6000000-0000-4000-8000-000000000001
-- BOM root               b1000000-0000-4000-8000-000000000001
-- BOM control arm MAKE   b1000000-0000-4000-8000-000000000002
-- BOM wheel bearing BUY  b1000000-0000-4000-8000-000000000003
-- Supplier ACME          c1000000-0000-4000-8000-000000000001
-- Supplier NIPPON        c1000000-0000-4000-8000-000000000002
-- RFQ                    d1000000-0000-4000-8000-000000000001
-- RFQ line               d1000000-0000-4000-8000-000000000002
-- Quote ACME             d1000000-0000-4000-8000-000000000003
-- Should-cost baseline   e1000000-0000-4000-8000-000000000001
-- Target cost            e1000000-0000-4000-8000-000000000002
-- Packaging profile      e1000000-0000-4000-8000-000000000003
-- Users                  f1000000-0000-4000-8000-00000000000x

-- ---------- 1. Demo users (roles for every function) ----------
INSERT INTO app_user (
    user_id, username, email, password_hash, status, mfa_enabled,
    token_version, failed_login_attempts, password_updated_at, created_at
)
SELECT 'f1000000-0000-4000-8000-000000000001', 'demo.engineer', 'engineer@demo.auto',
       '$2b$10$TWZ.5vAxuEf/dDNqxblzr.JrLtk2NG0I63008RTvJbZOnGkEIV0pe',
       'Active', FALSE, 0, 0, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'demo.engineer');

INSERT INTO app_user (
    user_id, username, email, password_hash, status, mfa_enabled,
    token_version, failed_login_attempts, password_updated_at, created_at
)
SELECT 'f1000000-0000-4000-8000-000000000002', 'demo.manager', 'manager@demo.auto',
       '$2b$10$TWZ.5vAxuEf/dDNqxblzr.JrLtk2NG0I63008RTvJbZOnGkEIV0pe',
       'Active', FALSE, 0, 0, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'demo.manager');

INSERT INTO app_user (
    user_id, username, email, password_hash, status, mfa_enabled,
    token_version, failed_login_attempts, password_updated_at, created_at
)
SELECT 'f1000000-0000-4000-8000-000000000003', 'demo.buyer', 'buyer@demo.auto',
       '$2b$10$TWZ.5vAxuEf/dDNqxblzr.JrLtk2NG0I63008RTvJbZOnGkEIV0pe',
       'Active', FALSE, 0, 0, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'demo.buyer');

INSERT INTO app_user (
    user_id, username, email, password_hash, status, mfa_enabled,
    token_version, failed_login_attempts, password_updated_at, created_at
)
SELECT 'f1000000-0000-4000-8000-000000000004', 'demo.master', 'masterdata@demo.auto',
       '$2b$10$TWZ.5vAxuEf/dDNqxblzr.JrLtk2NG0I63008RTvJbZOnGkEIV0pe',
       'Active', FALSE, 0, 0, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'demo.master');

INSERT IGNORE INTO user_role (user_id, role_id)
SELECT u.user_id, r.role_id FROM app_user u, role r
WHERE u.username = 'demo.engineer' AND r.role_code = 'COSTING_ENGINEER';

INSERT IGNORE INTO user_role (user_id, role_id)
SELECT u.user_id, r.role_id FROM app_user u, role r
WHERE u.username = 'demo.manager' AND r.role_code = 'COSTING_MANAGER';

INSERT IGNORE INTO user_role (user_id, role_id)
SELECT u.user_id, r.role_id FROM app_user u, role r
WHERE u.username = 'demo.buyer' AND r.role_code = 'PURCHASING_BUYER';

INSERT IGNORE INTO user_role (user_id, role_id)
SELECT u.user_id, r.role_id FROM app_user u, role r
WHERE u.username = 'demo.master' AND r.role_code = 'MASTER_DATA_ADMIN';

-- ---------- 2. Plants ----------
INSERT IGNORE INTO plant_master (plant_id, plant_code, country, statutory_duty_notes, freight_zone)
VALUES
    ('a1000000-0000-4000-8000-000000000001', 'DET-01', 'US', 'USMCA preferential origin possible', 'NA-EAST'),
    ('a1000000-0000-4000-8000-000000000002', 'PUN-01', 'IN', 'IGST applicable on domestic', 'IN-WEST');

-- ---------- 3. Vehicle program (multi-currency target EUR for demo FX) ----------
INSERT IGNORE INTO vehicle_program (
    program_id, program_code, model_name, platform_code, sourcing_region,
    lifetime_volume, target_cost, target_currency, status, created_at
) VALUES (
    'a2000000-0000-4000-8000-000000000001',
    'EV-CX-2027',
    'Compact Electric Crossover 2027',
    'GEP3',
    'NA + India',
    450000.00,
    18500.000000,
    'USD',
    'Active',
    UTC_TIMESTAMP(6)
);

-- ---------- 4. Materials (steel + aluminum) ----------
INSERT IGNORE INTO material_master (
    record_id, code, description, rate, uom, plant_id, effective_from, effective_to,
    version_number, status, grade, specification_standard, density, scrap_recovery_rate,
    material_form, index_linked_flag, created_at
) VALUES
(
    'a3000000-0000-4000-8000-000000000001',
    'CRS-1010',
    'Cold rolled steel sheet 1.5mm — control arms',
    0.920000, 'KG', 'a1000000-0000-4000-8000-000000000001',
    '2025-01-01', NULL, 1, 'ACTIVE',
    '1010', 'ASTM A1008', 7.850000, 0.150000, 'SHEET', FALSE, UTC_TIMESTAMP(6)
),
(
    'a3000000-0000-4000-8000-000000000002',
    'AL-6061',
    'Aluminum 6061-T6 extrusion',
    3.450000, 'KG', 'a1000000-0000-4000-8000-000000000001',
    '2025-01-01', NULL, 1, 'ACTIVE',
    '6061-T6', 'ASTM B221', 2.700000, 0.200000, 'EXTRUSION', FALSE, UTC_TIMESTAMP(6)
);

-- ---------- 5. Machine + labor + overhead ----------
INSERT IGNORE INTO machine_master (
    record_id, code, description, rate, uom, plant_id, effective_from, version_number, status,
    machine_type, capacity, depreciation_life_years, capital_cost, annual_operating_hours,
    power_cost_per_hour, maintenance_cost_per_hour, floor_space_cost_per_hour, shift_pattern, created_at
) VALUES (
    'a4000000-0000-4000-8000-000000000001',
    'PRESS-800T',
    '800-ton transfer press — stamping line 3',
    85.500000, 'HR', 'a1000000-0000-4000-8000-000000000001',
    '2025-01-01', 1, 'ACTIVE',
    'TRANSFER_PRESS', '800T', 12.00, 2400000.000000, 4800.00,
    18.000000, 12.500000, 6.000000, '3-SHIFT', UTC_TIMESTAMP(6)
);

INSERT IGNORE INTO labor_master (
    record_id, code, description, rate, uom, plant_id, effective_from, version_number, status,
    operator_grade, shift_type, country_region, statutory_burden_pct, created_at
) VALUES (
    'a5000000-0000-4000-8000-000000000001',
    'OP-STAMP',
    'Stamping operator Grade B',
    28.750000, 'HR', 'a1000000-0000-4000-8000-000000000001',
    '2025-01-01', 1, 'ACTIVE',
    'GRADE_B', 'DAY', 'US-MI', 0.350000, UTC_TIMESTAMP(6)
);

INSERT IGNORE INTO overhead_pool (
    record_id, code, description, rate, uom, plant_id, effective_from, version_number, status,
    pool_category, allocation_base, period, created_at
) VALUES (
    'a6000000-0000-4000-8000-000000000001',
    'OH-MFG-DET',
    'Detroit plant manufacturing overhead pool',
    42.000000, 'HR', 'a1000000-0000-4000-8000-000000000001',
    '2025-01-01', 1, 'ACTIVE',
    'MFG', 'DIRECT_LABOR_HOURS', '2026', UTC_TIMESTAMP(6)
);

-- ---------- 6. BOM tree: Program root → MAKE control arm + BUY bearing ----------
INSERT IGNORE INTO bom_node (
    bom_node_id, parent_node_id, program_id, part_number, revision, quantity_per, uom,
    make_buy_flag, ecn_reference, net_weight, scrap_pct, status, what_if_branch_flag, override_flag,
    material_code, created_at
) VALUES
(
    'b1000000-0000-4000-8000-000000000001', NULL,
    'a2000000-0000-4000-8000-000000000001',
    'ASM-FR-SUSP', 'A', 1.000000, 'EA', 'MAKE', 'ECN-2026-1001', NULL, NULL, 'RELEASED', FALSE, FALSE, NULL, UTC_TIMESTAMP(6)
),
(
    'b1000000-0000-4000-8000-000000000002',
    'b1000000-0000-4000-8000-000000000001',
    'a2000000-0000-4000-8000-000000000001',
    'CA-LH-FR', 'B', 1.000000, 'EA', 'MAKE', 'ECN-2026-1002', 2.450000, 0.050000, 'RELEASED', FALSE, FALSE,
    'CRS-1010', UTC_TIMESTAMP(6)
),
(
    'b1000000-0000-4000-8000-000000000003',
    'b1000000-0000-4000-8000-000000000001',
    'a2000000-0000-4000-8000-000000000001',
    'BRG-HUB-FR', 'A', 2.000000, 'EA', 'BUY', 'ECN-2026-1003', 0.850000, NULL, 'RELEASED', FALSE, FALSE,
    NULL, UTC_TIMESTAMP(6)
);

-- ---------- 7. Material cost calculation on MAKE leaf ----------
INSERT IGNORE INTO material_cost_calculation (
    calculation_id, bom_node_id, material_code, net_weight, scrap_pct,
    unit_rate, pricing_basis, gross_weight, material_cost, scrap_recovery_credit,
    calculation_basis, calculated_at, created_at
)
SELECT
    'e2000000-0000-4000-8000-000000000001',
    'b1000000-0000-4000-8000-000000000002',
    'CRS-1010',
    2.450000, 0.050000,
    0.920000, 'MASTER_RATE',
    2.578947,
    2.372632,
    0.118632,
    'Demo: net 2.45 kg CRS-1010 @ 0.92/kg, scrap 5%',
    UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
WHERE EXISTS (SELECT 1 FROM information_schema.tables
              WHERE table_schema = DATABASE() AND table_name = 'material_cost_calculation');

-- ---------- 8. Packaging profile on MAKE leaf ----------
INSERT IGNORE INTO packaging_logistics_profile (
    profile_id, bom_node_id, profile_label, packaging_type,
    primary_pack_cost_per_piece, secondary_pack_cost_per_piece,
    returnable_container_cost, container_life_cycles,
    net_weight_kg, freight_mode, freight_rate_per_kg, fixed_freight_per_unit,
    duty_pct, origin_plant_id, destination_freight_zone, notes, active, created_at
) VALUES (
    'e1000000-0000-4000-8000-000000000003',
    'b1000000-0000-4000-8000-000000000002',
    'Standard expendable — DET to plant warehouse',
    'EXPENDABLE_CARTON',
    0.180000, 0.050000,
    NULL, NULL,
    2.450000, 'OUTBOUND', 0.120000, 0.250000,
    0.000000, 'a1000000-0000-4000-8000-000000000001', 'NA-EAST',
    'Demo packaging for control arm', TRUE, UTC_TIMESTAMP(6)
);

-- ---------- 9. Should-cost baseline scenario (MAKE control arm) ----------
INSERT IGNORE INTO should_cost_build_up (
    build_up_id, bom_node_id, scenario_label,
    material_cost, process_cost, tooling_cost, packaging_freight_cost,
    overhead_cost, sga_cost, profit_margin, total_cost,
    stale_inputs_flag, locked, calculation_basis, calculated_at, approval_status, created_at
) VALUES (
    'e1000000-0000-4000-8000-000000000001',
    'b1000000-0000-4000-8000-000000000002',
    'Baseline DET stamping',
    2.370000, 1.850000, 0.420000, 0.770000,
    1.100000, 0.450000, 0.350000, 7.310000,
    FALSE, FALSE,
    'Demo nine-element build-up for CA-LH-FR',
    UTC_TIMESTAMP(6), 'DRAFT', UTC_TIMESTAMP(6)
);

-- ---------- 10. Target cost + variance snapshot ----------
INSERT IGNORE INTO target_cost_record (
    target_cost_id, bom_node_id, program_id, target_amount, currency_code,
    effective_from, status, approval_status, notes, created_at
) VALUES (
    'e1000000-0000-4000-8000-000000000002',
    'b1000000-0000-4000-8000-000000000002',
    'a2000000-0000-4000-8000-000000000001',
    6.950000, 'USD', CURDATE(), 'ACTIVE', 'APPROVED',
    'Program target for LH front control arm', UTC_TIMESTAMP(6)
);

INSERT IGNORE INTO target_cost_variance (
    variance_id, target_cost_id, bom_node_id, program_id,
    target_amount, actual_amount, actual_source, actual_source_id,
    absolute_variance, percentage_variance, exceeds_threshold, threshold_pct,
    calculation_basis, calculated_at, created_at
) VALUES (
    'e1000000-0000-4000-8000-000000000004',
    'e1000000-0000-4000-8000-000000000002',
    'b1000000-0000-4000-8000-000000000002',
    'a2000000-0000-4000-8000-000000000001',
    6.950000, 7.310000, 'SHOULD_COST', 'e1000000-0000-4000-8000-000000000001',
    0.360000, 5.179856, TRUE, 5.000000,
    'Demo variance: should-cost vs target',
    UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
);

-- ---------- 11. Suppliers + RFQ + accepted quote (BUY bearing) ----------
INSERT IGNORE INTO supplier_master (
    supplier_id, supplier_code, supplier_name, country_code, currency_code, status, notes, created_at
) VALUES
(
    'c1000000-0000-4000-8000-000000000001',
    'ACME-BRG',
    'ACME Bearing Systems LLC',
    'US', 'USD', 'ACTIVE', 'Preferred NA hub bearings', UTC_TIMESTAMP(6)
),
(
    'c1000000-0000-4000-8000-000000000002',
    'NIPPON-BRG',
    'Nippon Precision Bearings Co.',
    'JP', 'USD', 'ACTIVE', 'Alternate Asia source', UTC_TIMESTAMP(6)
);

INSERT IGNORE INTO rfq_event (
    rfq_id, program_id, rfq_number, title, status, due_date, currency_code, notes, created_at
) VALUES (
    'd1000000-0000-4000-8000-000000000001',
    'a2000000-0000-4000-8000-000000000001',
    'RFQ-2026-0042',
    'Front hub bearing — EV-CX-2027',
    'AWARDED', DATE_ADD(CURDATE(), INTERVAL 14 DAY), 'USD',
    'Demo RFQ with accepted ACME quote', UTC_TIMESTAMP(6)
);

INSERT IGNORE INTO rfq_line (
    rfq_line_id, rfq_id, bom_node_id, line_number, quantity, uom, target_unit_price, created_at
) VALUES (
    'd1000000-0000-4000-8000-000000000002',
    'd1000000-0000-4000-8000-000000000001',
    'b1000000-0000-4000-8000-000000000003',
    1, 900000.000000, 'EA', 4.500000, UTC_TIMESTAMP(6)
);

INSERT IGNORE INTO supplier_quote (
    quote_id, rfq_id, rfq_line_id, supplier_id, bom_node_id,
    unit_price, currency_code, tooling_lump_sum, lead_time_days,
    valid_from, valid_to, status, negotiation_reference, notes, created_at, updated_at
) VALUES
(
    'd1000000-0000-4000-8000-000000000003',
    'd1000000-0000-4000-8000-000000000001',
    'd1000000-0000-4000-8000-000000000002',
    'c1000000-0000-4000-8000-000000000001',
    'b1000000-0000-4000-8000-000000000003',
    4.250000, 'USD', 125000.000000, 45,
    CURDATE(), DATE_ADD(CURDATE(), INTERVAL 180 DAY),
    'ACCEPTED', 'NEG-2026-ACME-01', 'Awarded demo quote', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
),
(
    'd1000000-0000-4000-8000-000000000004',
    'd1000000-0000-4000-8000-000000000001',
    'd1000000-0000-4000-8000-000000000002',
    'c1000000-0000-4000-8000-000000000002',
    'b1000000-0000-4000-8000-000000000003',
    4.100000, 'USD', 180000.000000, 75,
    CURDATE(), DATE_ADD(CURDATE(), INTERVAL 180 DAY),
    'REJECTED', NULL, 'Longer lead time — rejected in demo', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
);

-- ---------- 12. Sample notification for demo.manager ----------
INSERT INTO app_notification (
    notification_id, recipient_user_id, notification_type, title, body,
    entity_type, entity_id, severity, read_flag, created_at
)
SELECT
    'e1000000-0000-4000-8000-000000000010',
    u.user_id,
    'TARGET_VARIANCE',
    'Demo: Target variance exceeds threshold',
    'Control arm CA-LH-FR should-cost $7.31 vs target $6.95 (+5.2%). Open Target Costing or Cost Comparison.',
    'TargetCostRecord',
    'e1000000-0000-4000-8000-000000000002',
    'WARN',
    FALSE,
    UTC_TIMESTAMP(6)
FROM app_user u
WHERE u.username = 'demo.manager'
  AND NOT EXISTS (SELECT 1 FROM app_notification WHERE notification_id = 'e1000000-0000-4000-8000-000000000010');
