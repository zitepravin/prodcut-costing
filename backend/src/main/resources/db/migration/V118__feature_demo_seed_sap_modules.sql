-- =============================================================================
-- V118 Feature demo seed — SAP A–E + market-gap modules (6–7 records each where useful)
-- Depends on V18 core demo (program EV-CX-2027, BOM nodes, should-cost, materials)
-- Fixed IDs prefix g1… for documentation in DEMO_WALKTHROUGH.md
-- Password for demo users (V18): ChangeMe!Now1
-- Idempotent: INSERT IGNORE / NOT EXISTS
-- =============================================================================

-- BOM / program IDs from V18
-- program: a2000000-0000-4000-8000-000000000001
-- BOM control arm: b1000000-0000-4000-8000-000000000002
-- BOM bearing BUY: b1000000-0000-4000-8000-000000000003
-- should-cost: e1000000-0000-4000-8000-000000000001

-- ---------- A1 Costing variants (7) ----------
INSERT IGNORE INTO costing_variant (
    variant_id, variant_code, variant_name, status, valuation_strategy,
    material_price_source, activity_price_source, quantity_structure_source,
    currency_code, lot_size, scrap_pct, yield_pct, description, created_at
) VALUES
('g1000001-0000-4000-8000-000000000001', 'STD-PROD', 'Standard production', 'ACTIVE', 'STANDARD',
 'MATERIAL_MASTER', 'MACHINE_LABOR_MASTER', 'BOM_AND_ROUTING', 'USD', 1.000000, 0.020000, 0.980000,
 'Demo: default production valuation', UTC_TIMESTAMP(6)),
('g1000001-0000-4000-8000-000000000002', 'PLAN-YR', 'Planned year cost', 'ACTIVE', 'PLANNED',
 'MATERIAL_MASTER', 'MACHINE_LABOR_MASTER', 'BOM_AND_ROUTING', 'USD', 1000.000000, 0.015000, 0.985000,
 'Demo: annual planning lot 1000', UTC_TIMESTAMP(6)),
('g1000001-0000-4000-8000-000000000003', 'QUOTE-BUY', 'Quote comparison', 'ACTIVE', 'QUOTE',
 'SUPPLIER_QUOTE', 'MACHINE_LABOR_MASTER', 'BOM_AND_ROUTING', 'USD', 1.000000, 0.000000, 1.000000,
 'Demo: buy parts vs quote', UTC_TIMESTAMP(6)),
('g1000001-0000-4000-8000-000000000004', 'SIM-LIGHT', 'Lightweight what-if', 'DRAFT', 'STANDARD',
 'MATERIAL_MASTER', 'MACHINE_LABOR_MASTER', 'BOM_AND_ROUTING', 'USD', 1.000000, 0.030000, 0.970000,
 'Demo: higher scrap draft', UTC_TIMESTAMP(6)),
('g1000001-0000-4000-8000-000000000005', 'EXPORT-EUR', 'Export EUR costing', 'ACTIVE', 'STANDARD',
 'MATERIAL_MASTER', 'MACHINE_LABOR_MASTER', 'BOM_AND_ROUTING', 'EUR', 1.000000, 0.020000, 0.980000,
 'Demo: EUR reporting currency', UTC_TIMESTAMP(6)),
('g1000001-0000-4000-8000-000000000006', 'PROTO', 'Prototype lot', 'ACTIVE', 'STANDARD',
 'MATERIAL_MASTER', 'MACHINE_LABOR_MASTER', 'BOM_AND_ROUTING', 'USD', 50.000000, 0.050000, 0.950000,
 'Demo: small lot high scrap', UTC_TIMESTAMP(6)),
('g1000001-0000-4000-8000-000000000007', 'LEGACY', 'Superseded variant', 'INACTIVE', 'STANDARD',
 'MATERIAL_MASTER', 'MACHINE_LABOR_MASTER', 'BOM_AND_ROUTING', 'USD', 1.000000, 0.000000, 1.000000,
 'Demo: inactive historical', UTC_TIMESTAMP(6));

-- ---------- A6 Costing sheets (if table exists from V109) ----------
INSERT IGNORE INTO costing_sheet (
    sheet_id, sheet_code, sheet_name, status, description, created_at
) VALUES (
    'g1000006-0000-4000-8000-000000000001', 'MFG-STD', 'Manufacturing standard sheet', 'ACTIVE',
    'Demo sheet: sequential overhead rules', UTC_TIMESTAMP(6)
);

-- ---------- B4 Scenarios (6) on control-arm BOM ----------
INSERT IGNORE INTO costing_scenario (
    scenario_id, bom_node_id, program_id, scenario_code, scenario_name, scenario_type,
    status, should_cost_build_up_id, notes, locked, created_at
) VALUES
('g1000004-0000-4000-8000-000000000001', 'b1000000-0000-4000-8000-000000000002',
 'a2000000-0000-4000-8000-000000000001', 'BASE', 'Base production', 'BASE',
 'APPROVED', 'e1000000-0000-4000-8000-000000000001', 'Demo locked baseline', TRUE, UTC_TIMESTAMP(6)),
('g1000004-0000-4000-8000-000000000002', 'b1000000-0000-4000-8000-000000000002',
 'a2000000-0000-4000-8000-000000000001', 'SUP-A', 'Supplier A buy path', 'SUPPLIER_A',
 'ACTIVE', NULL, 'Demo supplier alternative', FALSE, UTC_TIMESTAMP(6)),
('g1000004-0000-4000-8000-000000000003', 'b1000000-0000-4000-8000-000000000002',
 'a2000000-0000-4000-8000-000000000001', 'MAKE', 'Full make', 'MAKE',
 'DRAFT', 'e1000000-0000-4000-8000-000000000001', 'Demo make', FALSE, UTC_TIMESTAMP(6)),
('g1000004-0000-4000-8000-000000000004', 'b1000000-0000-4000-8000-000000000002',
 'a2000000-0000-4000-8000-000000000001', 'ECN-12', 'ECN thickness change', 'ENGINEERING_CHANGE',
 'DRAFT', NULL, 'Demo ECN branch', FALSE, UTC_TIMESTAMP(6)),
('g1000004-0000-4000-8000-000000000005', 'b1000000-0000-4000-8000-000000000002',
 'a2000000-0000-4000-8000-000000000001', 'VOL-UP', 'Volume up 20%', 'VOLUME_CHANGE',
 'DRAFT', NULL, 'Demo volume', FALSE, UTC_TIMESTAMP(6)),
('g1000004-0000-4000-8000-000000000006', 'b1000000-0000-4000-8000-000000000002',
 'a2000000-0000-4000-8000-000000000001', 'FX-EUR', 'EUR FX shock', 'CURRENCY_CHANGE',
 'DRAFT', NULL, 'Demo FX', FALSE, UTC_TIMESTAMP(6));

-- B5 Additive lines on BASE scenario (6)
INSERT IGNORE INTO additive_cost_line (
    line_id, scenario_id, line_code, line_name, additive_type, amount, currency_code,
    amortized, amortization_volume, amount_per_unit, notes, created_at
) VALUES
('g1000005-0000-4000-8000-000000000001', 'g1000004-0000-4000-8000-000000000001',
 'PROTO', 'Prototype builds', 'PROTOTYPE', 25000.000000, 'USD', TRUE, 5000.000000, 5.000000, 'Demo', UTC_TIMESTAMP(6)),
('g1000005-0000-4000-8000-000000000002', 'g1000004-0000-4000-8000-000000000001',
 'LAUNCH', 'Launch premium', 'LAUNCH_PREMIUM', 12000.000000, 'USD', TRUE, 10000.000000, 1.200000, 'Demo', UTC_TIMESTAMP(6)),
('g1000005-0000-4000-8000-000000000003', 'g1000004-0000-4000-8000-000000000001',
 'STOOL', 'Special tooling', 'SPECIAL_TOOLING', 80000.000000, 'USD', TRUE, 20000.000000, 4.000000, 'Demo', UTC_TIMESTAMP(6)),
('g1000005-0000-4000-8000-000000000004', 'g1000004-0000-4000-8000-000000000001',
 'ENG', 'One-time engineering', 'ONE_TIME_ENGINEERING', 15000.000000, 'USD', FALSE, NULL, NULL, 'Demo', UTC_TIMESTAMP(6)),
('g1000005-0000-4000-8000-000000000005', 'g1000004-0000-4000-8000-000000000001',
 'FRT', 'Special freight', 'SPECIAL_FREIGHT', 3500.000000, 'USD', FALSE, NULL, NULL, 'Demo', UTC_TIMESTAMP(6)),
('g1000005-0000-4000-8000-000000000006', 'g1000004-0000-4000-8000-000000000001',
 'CUST', 'Customer-specific packaging', 'CUSTOMER_SPECIFIC', 2200.000000, 'USD', TRUE, 11000.000000, 0.200000, 'Demo', UTC_TIMESTAMP(6));

-- ---------- C1 Standard costs (lifecycle samples) ----------
INSERT IGNORE INTO standard_cost (
    standard_cost_id, bom_node_id, program_id, version_no, status, source_type, source_ref_id,
    unit_cost, currency_code, material_cost, process_cost, notes, created_at
) VALUES
('g10000c1-0000-4000-8000-000000000001', 'b1000000-0000-4000-8000-000000000002',
 'a2000000-0000-4000-8000-000000000001', 1, 'SUPERSEDED', 'SHOULD_COST', 'e1000000-0000-4000-8000-000000000001',
 7.100000, 'USD', 3.200000, 2.500000, 'Demo v1 superseded', UTC_TIMESTAMP(6)),
('g10000c1-0000-4000-8000-000000000002', 'b1000000-0000-4000-8000-000000000002',
 'a2000000-0000-4000-8000-000000000001', 2, 'RELEASED', 'SHOULD_COST', 'e1000000-0000-4000-8000-000000000001',
 7.310000, 'USD', 3.250000, 2.550000, 'Demo v2 current released', UTC_TIMESTAMP(6)),
('g10000c1-0000-4000-8000-000000000003', 'b1000000-0000-4000-8000-000000000002',
 'a2000000-0000-4000-8000-000000000001', 3, 'DRAFT', 'SHOULD_COST', 'e1000000-0000-4000-8000-000000000001',
 7.050000, 'USD', 3.100000, 2.400000, 'Demo v3 draft improvement', UTC_TIMESTAMP(6));

-- ---------- C2 Actual costs (6 imports) ----------
INSERT IGNORE INTO actual_cost_record (
    actual_cost_id, bom_node_id, program_id, source_system, source_ref, period_code,
    quantity, unit_cost, total_cost, currency_code, notes, imported_at, created_at
) VALUES
('g10000c2-0000-4000-8000-000000000001', 'b1000000-0000-4000-8000-000000000002',
 'a2000000-0000-4000-8000-000000000001', 'IMPORT', 'PO-1001', '2026-01',
 1000.000000, 7.450000, 7450.000000, 'USD', 'Demo Jan actual', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
('g10000c2-0000-4000-8000-000000000002', 'b1000000-0000-4000-8000-000000000002',
 'a2000000-0000-4000-8000-000000000001', 'IMPORT', 'PO-1002', '2026-02',
 1200.000000, 7.380000, 8856.000000, 'USD', 'Demo Feb actual', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
('g10000c2-0000-4000-8000-000000000003', 'b1000000-0000-4000-8000-000000000002',
 'a2000000-0000-4000-8000-000000000001', 'IMPORT', 'PO-1003', '2026-03',
 1100.000000, 7.520000, 8272.000000, 'USD', 'Demo Mar actual', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
('g10000c2-0000-4000-8000-000000000004', 'b1000000-0000-4000-8000-000000000002',
 'a2000000-0000-4000-8000-000000000001', 'IMPORT', 'PO-1004', '2026-04',
 1300.000000, 7.290000, 9477.000000, 'USD', 'Demo Apr actual', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
('g10000c2-0000-4000-8000-000000000005', 'b1000000-0000-4000-8000-000000000002',
 'a2000000-0000-4000-8000-000000000001', 'SAP_STAGING', 'SAP-ACT-55', '2026-05',
 900.000000, 7.610000, 6849.000000, 'USD', 'Demo May SAP-sourced actual', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
('g10000c2-0000-4000-8000-000000000006', 'b1000000-0000-4000-8000-000000000002',
 'a2000000-0000-4000-8000-000000000001', 'IMPORT', 'PO-1006', '2026-06',
 1400.000000, 7.330000, 10262.000000, 'USD', 'Demo Jun actual', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));

-- ---------- C4 Valuation policies (3) ----------
INSERT IGNORE INTO valuation_currency_policy (
    policy_id, policy_code, policy_name, company_currency, group_currency,
    transaction_currency, costing_currency, valuation_date_policy, active, notes, created_at
) VALUES
('g10000c4-0000-4000-8000-000000000001', 'NA-STD', 'North America standard',
 'USD', 'USD', 'USD', 'USD', 'COSTING_DATE', TRUE, 'Demo NA', UTC_TIMESTAMP(6)),
('g10000c4-0000-4000-8000-000000000002', 'EU-GRP', 'EU group reporting',
 'EUR', 'EUR', 'USD', 'EUR', 'DOCUMENT_DATE', TRUE, 'Demo EU', UTC_TIMESTAMP(6)),
('g10000c4-0000-4000-8000-000000000003', 'IN-LOCAL', 'India local costing',
 'INR', 'USD', 'INR', 'INR', 'TODAY', TRUE, 'Demo IN', UTC_TIMESTAMP(6));

-- ---------- D1 Activity types (7) ----------
INSERT IGNORE INTO activity_type (
    activity_type_id, activity_code, activity_name, category, unit_of_measure,
    default_rate, currency_code, cost_center_code, active, notes, created_at
) VALUES
('g10000d1-0000-4000-8000-000000000001', 'PRESS-HR', 'Press machine hour', 'MACHINE', 'HR', 85.000000, 'USD', 'CC-PRESS', TRUE, 'Demo', UTC_TIMESTAMP(6)),
('g10000d1-0000-4000-8000-000000000002', 'CNC-HR', 'CNC machine hour', 'MACHINE', 'HR', 110.000000, 'USD', 'CC-CNC', TRUE, 'Demo', UTC_TIMESTAMP(6)),
('g10000d1-0000-4000-8000-000000000003', 'LABOR-DIR', 'Direct labor hour', 'LABOR', 'HR', 42.000000, 'USD', 'CC-ASSY', TRUE, 'Demo', UTC_TIMESTAMP(6)),
('g10000d1-0000-4000-8000-000000000004', 'SETUP', 'Setup hour', 'SETUP', 'HR', 55.000000, 'USD', 'CC-PRESS', TRUE, 'Demo', UTC_TIMESTAMP(6)),
('g10000d1-0000-4000-8000-000000000005', 'ENERGY', 'Energy per kWh', 'ENERGY', 'KWH', 0.120000, 'USD', 'CC-UTIL', TRUE, 'Demo', UTC_TIMESTAMP(6)),
('g10000d1-0000-4000-8000-000000000006', 'QA-INSP', 'Inspection hour', 'QUALITY', 'HR', 48.000000, 'USD', 'CC-QA', TRUE, 'Demo', UTC_TIMESTAMP(6)),
('g10000d1-0000-4000-8000-000000000007', 'WELD-HR', 'Weld cell hour', 'MACHINE', 'HR', 95.000000, 'USD', 'CC-WELD', TRUE, 'Demo', UTC_TIMESTAMP(6));

-- ---------- D2 Capacity centers + loads ----------
INSERT IGNORE INTO capacity_center (
    capacity_center_id, center_code, center_name, plant_code, available_hours,
    period_code, efficiency_pct, active, notes, created_at
) VALUES
('g10000d2-0000-4000-8000-000000000001', 'PRESS-LINE-1', 'Press line 1', 'DET-01', 160.000000, '2026-08', 0.900000, TRUE, 'Demo', UTC_TIMESTAMP(6)),
('g10000d2-0000-4000-8000-000000000002', 'CNC-CELL-A', 'CNC cell A', 'DET-01', 140.000000, '2026-08', 0.850000, TRUE, 'Demo', UTC_TIMESTAMP(6)),
('g10000d2-0000-4000-8000-000000000003', 'WELD-CELL-2', 'Weld cell 2', 'DET-01', 150.000000, '2026-08', 0.880000, TRUE, 'Demo', UTC_TIMESTAMP(6)),
('g10000d2-0000-4000-8000-000000000004', 'ASSY-1', 'Assembly line 1', 'DET-01', 170.000000, '2026-08', 0.920000, TRUE, 'Demo', UTC_TIMESTAMP(6)),
('g10000d2-0000-4000-8000-000000000005', 'QA-LAB', 'QA lab', 'DET-01', 80.000000, '2026-08', 1.000000, TRUE, 'Demo', UTC_TIMESTAMP(6)),
('g10000d2-0000-4000-8000-000000000006', 'PRESS-PUN', 'Press Pune', 'PUN-01', 160.000000, '2026-08', 0.870000, TRUE, 'Demo', UTC_TIMESTAMP(6));

INSERT IGNORE INTO capacity_load (
    load_id, capacity_center_id, activity_type_id, required_hours, load_label, created_at
) VALUES
('g10000d2-0000-4000-8000-000000000011', 'g10000d2-0000-4000-8000-000000000001',
 'g10000d1-0000-4000-8000-000000000001', 90.000000, 'EV-CX control arm stamp', UTC_TIMESTAMP(6)),
('g10000d2-0000-4000-8000-000000000012', 'g10000d2-0000-4000-8000-000000000001',
 'g10000d1-0000-4000-8000-000000000004', 25.000000, 'Setup blocks', UTC_TIMESTAMP(6)),
('g10000d2-0000-4000-8000-000000000013', 'g10000d2-0000-4000-8000-000000000002',
 'g10000d1-0000-4000-8000-000000000002', 110.000000, 'CNC finish', UTC_TIMESTAMP(6)),
('g10000d2-0000-4000-8000-000000000014', 'g10000d2-0000-4000-8000-000000000003',
 'g10000d1-0000-4000-8000-000000000007', 70.000000, 'Weld brackets', UTC_TIMESTAMP(6)),
('g10000d2-0000-4000-8000-000000000015', 'g10000d2-0000-4000-8000-000000000005',
 'g10000d1-0000-4000-8000-000000000006', 40.000000, 'Incoming inspection', UTC_TIMESTAMP(6)),
('g10000d2-0000-4000-8000-000000000016', 'g10000d2-0000-4000-8000-000000000004',
 'g10000d1-0000-4000-8000-000000000003', 100.000000, 'Final assembly labor', UTC_TIMESTAMP(6));

-- ---------- E3 ECN impacts (sample analyzed) ----------
INSERT IGNORE INTO ecn_cost_impact (
    impact_id, ecn_number, ecn_title, program_id, bom_node_id,
    baseline_build_up_id, baseline_unit_cost, proposed_unit_cost, unit_delta,
    annual_volume, annual_program_delta, currency_code, status, impact_summary, notes, created_at
) VALUES
('g10000e3-0000-4000-8000-000000000001', 'ECN-2026-0012', 'Arm thickness +0.2mm',
 'a2000000-0000-4000-8000-000000000001', 'b1000000-0000-4000-8000-000000000002',
 'e1000000-0000-4000-8000-000000000001', 7.310000, 7.550000, 0.240000,
 90000.000000, 21600.000000, 'USD', 'ANALYZED',
 'ECN ECN-2026-0012: unit delta 0.24; annual program impact 21600 at volume 90000', 'Demo', UTC_TIMESTAMP(6)),
('g10000e3-0000-4000-8000-000000000002', 'ECN-2026-0018', 'Hole pattern change',
 'a2000000-0000-4000-8000-000000000001', 'b1000000-0000-4000-8000-000000000002',
 'e1000000-0000-4000-8000-000000000001', 7.310000, 7.280000, -0.030000,
 90000.000000, -2700.000000, 'USD', 'ANALYZED',
 'ECN ECN-2026-0018: unit delta -0.03; annual savings demo', 'Demo', UTC_TIMESTAMP(6));

-- ---------- E4 What-if simulations ----------
INSERT IGNORE INTO whatif_simulation (
    simulation_id, bom_node_id, program_id, label, baseline_unit_cost, baseline_currency,
    material_pct_change, volume_pct_change, fx_pct_change, process_pct_change,
    simulated_unit_cost, unit_delta, annual_volume, annual_delta, calculation_basis, status, created_at
) VALUES
('g10000e4-0000-4000-8000-000000000001', 'b1000000-0000-4000-8000-000000000002',
 'a2000000-0000-4000-8000-000000000001', 'Steel +10%', 7.310000, 'USD',
 10.000000, 0.000000, 0.000000, 0.000000,
 7.560000, 0.250000, 90000.000000, 22500.000000, 'Demo material +10%', 'COMPLETED', UTC_TIMESTAMP(6)),
('g10000e4-0000-4000-8000-000000000002', 'b1000000-0000-4000-8000-000000000002',
 'a2000000-0000-4000-8000-000000000001', 'Volume -20%', 7.310000, 'USD',
 0.000000, -20.000000, 0.000000, 0.000000,
 7.310000, 0.000000, 72000.000000, 0.000000, 'Demo volume down — unit unchanged', 'COMPLETED', UTC_TIMESTAMP(6)),
('g10000e4-0000-4000-8000-000000000003', 'b1000000-0000-4000-8000-000000000002',
 'a2000000-0000-4000-8000-000000000001', 'FX +5% + process +3%', 7.310000, 'USD',
 0.000000, 0.000000, 5.000000, 3.000000,
 7.680000, 0.370000, 90000.000000, 33300.000000, 'Demo combined shock', 'COMPLETED', UTC_TIMESTAMP(6));

-- ---------- PROCESS emission factor sample ----------
INSERT IGNORE INTO emission_factor (
    factor_id, factor_code, factor_name, category, co2e_value, unit, source_type,
    version_no, status, created_at
) VALUES
('g10000em-0000-4000-8000-000000000001', 'PROC-GRID-US', 'US grid process energy',
 'PROCESS', 0.420000, 'KG_CO2E_PER_HOUR', 'MANUAL', 1, 'ACTIVE', UTC_TIMESTAMP(6)),
('g10000em-0000-4000-8000-000000000002', 'MAT-STEEL-AVG', 'Average steel CO2e',
 'MATERIAL', 1.850000, 'KG_CO2E_PER_KG', 'MANUAL', 1, 'ACTIVE', UTC_TIMESTAMP(6));
