-- =============================================================================
-- V168 Demo Volume Extension — schema-aligned volume fill for role-wise demo
-- Extends V18 + V166 + V167. Idempotent fixed UUIDs.
-- Password unchanged: ChangeMe!Now1
-- =============================================================================

SET @cust_default := IFNULL((SELECT customer_id FROM platform_customer WHERE customer_code = 'DEFAULT' LIMIT 1),
                            '00000000-0000-0000-0000-000000000001');
SET @cust_aurora  := IFNULL((SELECT customer_id FROM platform_customer WHERE customer_code = 'AURORA' LIMIT 1), @cust_default);
SET @cust_horizon := IFNULL((SELECT customer_id FROM platform_customer WHERE customer_code = 'HORIZON' LIMIT 1), @cust_default);
SET @plant_aur    := 'a1000000-0000-4000-8000-000000000011';
SET @plant_def    := 'a1000000-0000-4000-8000-000000000001';

-- ---------------------------------------------------------------------------
-- 1. MATERIALS (AURORA rich catalog >= 30 total with prior seeds)
-- ---------------------------------------------------------------------------
INSERT INTO material_master (
    record_id, customer_id, code, description, rate, uom, plant_id,
    effective_from, version_number, status, grade, specification_standard,
    density, scrap_recovery_rate, material_form, index_linked_flag, created_at
)
SELECT v.record_id, @cust_aurora, v.code, v.description, v.rate, 'KG', @plant_aur,
       '2025-01-01', 1, 'ACTIVE', v.grade, v.spec, v.density, v.scrap, v.form, FALSE, UTC_TIMESTAMP(6)
FROM (
    SELECT 'a3000000-0000-4000-8000-000000000101' AS record_id, 'STL-ST12' AS code, 'Mild steel ST12 sheet' AS description, 0.780000 AS rate, 'ST12' AS grade, 'EN 10130' AS spec, 7.850000 AS density, 0.150000 AS scrap, 'SHEET' AS form
    UNION ALL SELECT 'a3000000-0000-4000-8000-000000000102', 'STL-DP600', 'Dual phase DP600 AHSS', 1.280000, 'DP600', 'VDA 239', 7.850000, 0.100000, 'COIL'
    UNION ALL SELECT 'a3000000-0000-4000-8000-000000000103', 'AL-6082', 'Aluminium 6082-T6 plate', 3.650000, '6082-T6', 'EN 573', 2.700000, 0.180000, 'PLATE'
    UNION ALL SELECT 'a3000000-0000-4000-8000-000000000104', 'AL-ADC12', 'Aluminium ADC12 die cast alloy', 2.950000, 'ADC12', 'JIS H5302', 2.740000, 0.220000, 'INGOT'
    UNION ALL SELECT 'a3000000-0000-4000-8000-000000000105', 'CI-GG25', 'Grey cast iron GG25', 1.450000, 'GG25', 'EN 1561', 7.200000, 0.080000, 'INGOT'
    UNION ALL SELECT 'a3000000-0000-4000-8000-000000000106', 'SS-304', 'Stainless steel 304 sheet', 4.850000, '304', 'ASTM A240', 8.000000, 0.200000, 'SHEET'
    UNION ALL SELECT 'a3000000-0000-4000-8000-000000000107', 'CU-C110', 'Copper C110 busbar', 9.200000, 'C110', 'ASTM B187', 8.960000, 0.300000, 'BAR'
    UNION ALL SELECT 'a3000000-0000-4000-8000-000000000108', 'PA66-GF30', 'PA66 GF30 engineering resin', 5.100000, 'PA66-GF30', 'ISO 527', 1.360000, 0.040000, 'PELLET'
    UNION ALL SELECT 'a3000000-0000-4000-8000-000000000109', 'PP-T20', 'PP talc 20% filled', 1.850000, 'PP-T20', 'ISO 1873', 1.040000, 0.030000, 'PELLET'
    UNION ALL SELECT 'a3000000-0000-4000-8000-000000000110', 'ABS-GP', 'ABS general purpose', 2.450000, 'ABS', 'ISO 2580', 1.050000, 0.050000, 'PELLET'
    UNION ALL SELECT 'a3000000-0000-4000-8000-000000000111', 'EPDM-70', 'EPDM rubber 70 Shore A', 3.200000, 'EPDM-70', 'ASTM D2000', 1.150000, 0.000000, 'COMPOUND'
    UNION ALL SELECT 'a3000000-0000-4000-8000-000000000112', 'RUB-NR', 'Natural rubber compound', 2.800000, 'NR', 'ASTM D2000', 0.920000, 0.000000, 'COMPOUND'
    UNION ALL SELECT 'a3000000-0000-4000-8000-000000000113', 'ADH-EPX', 'Epoxy structural adhesive', 18.500000, 'EPX', 'OEM-SPEC', 1.200000, 0.000000, 'LIQUID'
    UNION ALL SELECT 'a3000000-0000-4000-8000-000000000114', 'COAT-ED', 'E-coat primer', 12.000000, 'ED', 'OEM-SPEC', 1.300000, 0.000000, 'LIQUID'
    UNION ALL SELECT 'a3000000-0000-4000-8000-000000000115', 'BRG-6205', 'Deep groove bearing 6205', 4.500000, '6205', 'ISO 15', NULL, 0.000000, 'COMPONENT'
    UNION ALL SELECT 'a3000000-0000-4000-8000-000000000116', 'SEAL-NBR', 'NBR oil seal', 1.250000, 'NBR', 'DIN 3760', NULL, 0.000000, 'COMPONENT'
    UNION ALL SELECT 'a3000000-0000-4000-8000-000000000117', 'FAST-M8', 'M8x1.25 hex bolt 10.9', 0.085000, '10.9', 'ISO 898', NULL, 0.000000, 'COMPONENT'
    UNION ALL SELECT 'a3000000-0000-4000-8000-000000000118', 'SM-1.2', 'CRS sheet metal 1.2mm', 0.880000, 'CRS', 'ASTM A1008', 7.850000, 0.120000, 'SHEET'
    UNION ALL SELECT 'a3000000-0000-4000-8000-000000000119', 'FRG-42CrMo4', 'Forging alloy 42CrMo4', 2.150000, '42CrMo4', 'EN 10083', 7.850000, 0.100000, 'BILLET'
    UNION ALL SELECT 'a3000000-0000-4000-8000-000000000120', 'CST-A356', 'Casting alloy A356-T6', 3.100000, 'A356', 'ASTM B26', 2.670000, 0.150000, 'INGOT'
    UNION ALL SELECT 'a3000000-0000-4000-8000-000000000121', 'AL-7075', 'Aluminium 7075-T6 bar', 6.800000, '7075-T6', 'AMS 4045', 2.810000, 0.200000, 'BAR'
    UNION ALL SELECT 'a3000000-0000-4000-8000-000000000122', 'STL-S355', 'Structural steel S355', 0.950000, 'S355', 'EN 10025', 7.850000, 0.120000, 'PLATE'
    UNION ALL SELECT 'a3000000-0000-4000-8000-000000000123', 'PA6-NAT', 'PA6 natural resin', 3.750000, 'PA6', 'ISO 1874', 1.130000, 0.040000, 'PELLET'
    UNION ALL SELECT 'a3000000-0000-4000-8000-000000000124', 'PC-ABS', 'PC/ABS blend', 4.100000, 'PCABS', 'ISO 7391', 1.150000, 0.050000, 'PELLET'
    UNION ALL SELECT 'a3000000-0000-4000-8000-000000000125', 'POM-C', 'Acetal copolymer POM-C', 4.600000, 'POM-C', 'ISO 9988', 1.410000, 0.030000, 'PELLET'
) v
WHERE NOT EXISTS (SELECT 1 FROM material_master m WHERE m.code = v.code AND m.customer_id = @cust_aurora);

-- HORIZON materials
INSERT INTO material_master (
    record_id, customer_id, code, description, rate, uom, plant_id,
    effective_from, version_number, status, grade, density, scrap_recovery_rate,
    material_form, index_linked_flag, created_at
)
SELECT v.record_id, @cust_horizon, v.code, v.description, v.rate, 'KG', 'a1000000-0000-4000-8000-000000000021',
       '2025-01-01', 1, 'ACTIVE', v.grade, 7.850000, 0.120000, 'SHEET', FALSE, UTC_TIMESTAMP(6)
FROM (
    SELECT 'a3000000-0000-4000-8000-000000000201' AS record_id, 'HOR-CRS-1.5' AS code, 'Horizon CRS 1.5mm' AS description, 0.900000 AS rate, '1010' AS grade
    UNION ALL SELECT 'a3000000-0000-4000-8000-000000000202', 'HOR-AL-6063', 'Horizon AL 6063 extrusion', 3.200000, '6063'
    UNION ALL SELECT 'a3000000-0000-4000-8000-000000000203', 'HOR-PP', 'Horizon PP copolymer', 1.650000, 'PP'
    UNION ALL SELECT 'a3000000-0000-4000-8000-000000000204', 'HOR-EPDM', 'Horizon EPDM seal stock', 2.900000, 'EPDM'
    UNION ALL SELECT 'a3000000-0000-4000-8000-000000000205', 'HOR-SS304', 'Horizon SS304 strip', 4.500000, '304'
    UNION ALL SELECT 'a3000000-0000-4000-8000-000000000206', 'HOR-CAST', 'Horizon grey iron', 1.400000, 'GG20'
    UNION ALL SELECT 'a3000000-0000-4000-8000-000000000207', 'HOR-NYLON', 'Horizon PA6 GF15', 4.000000, 'PA6-GF15'
    UNION ALL SELECT 'a3000000-0000-4000-8000-000000000208', 'HOR-FAST', 'Horizon M6 fastener kit', 0.120000, '8.8'
    UNION ALL SELECT 'a3000000-0000-4000-8000-000000000209', 'HOR-RUB', 'Horizon rubber bumper', 2.500000, 'NR'
    UNION ALL SELECT 'a3000000-0000-4000-8000-000000000210', 'HOR-COAT', 'Horizon powder coat', 8.500000, 'PC'
    UNION ALL SELECT 'a3000000-0000-4000-8000-000000000211', 'HOR-ADH', 'Horizon adhesive', 15.000000, 'EPX'
    UNION ALL SELECT 'a3000000-0000-4000-8000-000000000212', 'HOR-SM', 'Horizon sheet metal 0.8', 0.820000, 'CRS'
    UNION ALL SELECT 'a3000000-0000-4000-8000-000000000213', 'HOR-ALU', 'Horizon ADC12', 2.800000, 'ADC12'
    UNION ALL SELECT 'a3000000-0000-4000-8000-000000000214', 'HOR-BRG', 'Horizon bearing 6004', 3.800000, '6004'
    UNION ALL SELECT 'a3000000-0000-4000-8000-000000000215', 'HOR-SEAL', 'Horizon oil seal', 1.100000, 'NBR'
) v
WHERE NOT EXISTS (SELECT 1 FROM material_master m WHERE m.code = v.code AND m.customer_id = @cust_horizon);

-- ---------------------------------------------------------------------------
-- 2. PROGRAMS (AURORA >= 5)
-- ---------------------------------------------------------------------------
INSERT INTO vehicle_program (
    program_id, customer_id, program_code, model_name, platform_code, sourcing_region,
    lifetime_volume, target_cost, target_currency, status, created_at
)
SELECT v.program_id, @cust_aurora, v.code, v.model, v.platform, v.region,
       v.vol, v.target, 'USD', 'Active', UTC_TIMESTAMP(6)
FROM (
    SELECT 'a2000000-0000-4000-8000-000000000101' AS program_id, 'EV-PLT-2027' AS code, 'EV Platform 2027' AS model, 'GEP4' AS platform, 'Global' AS region, 600000.00 AS vol, 16500.000000 AS target
    UNION ALL SELECT 'a2000000-0000-4000-8000-000000000102', 'SUV-C-2027', 'Compact SUV 2027', 'PLT-SUV-C', 'NA+EU', 320000.00, 14200.000000
    UNION ALL SELECT 'a2000000-0000-4000-8000-000000000103', 'CV-RFR-2028', 'Commercial Vehicle Refresh', 'PLT-CV', 'NA', 180000.00, 22000.000000
    UNION ALL SELECT 'a2000000-0000-4000-8000-000000000104', 'EPT-2028', 'Electric Powertrain Program', 'PLT-EPT', 'Global', 500000.00, 8500.000000
    UNION ALL SELECT 'a2000000-0000-4000-8000-000000000105', 'SED-PREM-2029', 'Premium Sedan Program', 'PLT-SED-P', 'EU+NA', 210000.00, 19500.000000
) v
WHERE NOT EXISTS (
    SELECT 1 FROM vehicle_program p WHERE p.program_code = v.code
       AND (p.customer_id = @cust_aurora OR p.customer_id IS NULL)
);

-- ---------------------------------------------------------------------------
-- 3. PRODUCT / BOM roots (AURORA product family >= 15 part roots)
-- ---------------------------------------------------------------------------
INSERT INTO bom_node (
    bom_node_id, customer_id, program_id, parent_node_id, part_number, revision,
    quantity_per, uom, make_buy_flag, status, what_if_branch_flag, override_flag, created_at
)
SELECT v.id, @cust_aurora, v.prog, NULL, v.pn, 'A', 1.000000, 'EA', 'MAKE', 'RELEASED', FALSE, FALSE, UTC_TIMESTAMP(6)
FROM (
    SELECT 'b1000000-0000-4000-8000-000000000101' AS id, 'a2000000-0000-4000-8000-000000000101' AS prog, 'BRAKE-CAL-ASM' AS pn
    UNION ALL SELECT 'b1000000-0000-4000-8000-000000000102', 'a2000000-0000-4000-8000-000000000101', 'STEER-KNUCKLE'
    UNION ALL SELECT 'b1000000-0000-4000-8000-000000000103', 'a2000000-0000-4000-8000-000000000101', 'ENG-MOUNT-BRKT'
    UNION ALL SELECT 'b1000000-0000-4000-8000-000000000104', 'a2000000-0000-4000-8000-000000000101', 'BATT-TRAY-ASM'
    UNION ALL SELECT 'b1000000-0000-4000-8000-000000000105', 'a2000000-0000-4000-8000-000000000102', 'XMSN-HOUSING'
    UNION ALL SELECT 'b1000000-0000-4000-8000-000000000106', 'a2000000-0000-4000-8000-000000000102', 'SUSP-CTRL-ARM'
    UNION ALL SELECT 'b1000000-0000-4000-8000-000000000107', 'a2000000-0000-4000-8000-000000000102', 'SEAT-BRACKET'
    UNION ALL SELECT 'b1000000-0000-4000-8000-000000000108', 'a2000000-0000-4000-8000-000000000103', 'IP-CROSS-BEAM'
    UNION ALL SELECT 'b1000000-0000-4000-8000-000000000109', 'a2000000-0000-4000-8000-000000000104', 'EMOTOR-HOUSING'
    UNION ALL SELECT 'b1000000-0000-4000-8000-000000000110', 'a2000000-0000-4000-8000-000000000105', 'AL-STRUCT-BRKT'
    UNION ALL SELECT 'b1000000-0000-4000-8000-000000000111', 'a2000000-0000-4000-8000-000000000101', 'HUB-CARRIER'
    UNION ALL SELECT 'b1000000-0000-4000-8000-000000000112', 'a2000000-0000-4000-8000-000000000102', 'SUBFRAME-FR'
    UNION ALL SELECT 'b1000000-0000-4000-8000-000000000113', 'a2000000-0000-4000-8000-000000000103', 'DOOR-HINGE-UH'
    UNION ALL SELECT 'b1000000-0000-4000-8000-000000000114', 'a2000000-0000-4000-8000-000000000104', 'COOL-PLATE-BAT'
    UNION ALL SELECT 'b1000000-0000-4000-8000-000000000115', 'a2000000-0000-4000-8000-000000000105', 'REAR-AXLE-BRKT'
) v
WHERE NOT EXISTS (SELECT 1 FROM bom_node b WHERE b.bom_node_id = v.id)
  AND EXISTS (SELECT 1 FROM vehicle_program p WHERE p.program_id = v.prog);

-- Child MAKE/BUY lines under a few roots
INSERT INTO bom_node (
    bom_node_id, customer_id, program_id, parent_node_id, part_number, revision,
    quantity_per, uom, make_buy_flag, material_code, net_weight, scrap_pct,
    status, what_if_branch_flag, override_flag, created_at
)
SELECT v.id, @cust_aurora, v.prog, v.parent, v.pn, 'A', v.qty, 'EA', v.mb, v.mat, v.wt, v.scrap,
       'RELEASED', FALSE, FALSE, UTC_TIMESTAMP(6)
FROM (
    SELECT 'b1000000-0000-4000-8000-000000000121' AS id, 'a2000000-0000-4000-8000-000000000101' AS prog,
           'b1000000-0000-4000-8000-000000000101' AS parent, 'CAL-BODY' AS pn, 1.000000 AS qty, 'MAKE' AS mb, 'AL-ADC12' AS mat, 2.100000 AS wt, 0.080000 AS scrap
    UNION ALL SELECT 'b1000000-0000-4000-8000-000000000122', 'a2000000-0000-4000-8000-000000000101',
           'b1000000-0000-4000-8000-000000000101', 'CAL-PISTON', 1.000000, 'BUY', NULL, 0.350000, NULL
    UNION ALL SELECT 'b1000000-0000-4000-8000-000000000123', 'a2000000-0000-4000-8000-000000000101',
           'b1000000-0000-4000-8000-000000000102', 'KNUCKLE-CAST', 1.000000, 'MAKE', 'CST-A356', 4.500000, 0.100000
    UNION ALL SELECT 'b1000000-0000-4000-8000-000000000124', 'a2000000-0000-4000-8000-000000000101',
           'b1000000-0000-4000-8000-000000000103', 'MOUNT-BRACKET', 1.000000, 'MAKE', 'STL-ST12', 1.200000, 0.050000
    UNION ALL SELECT 'b1000000-0000-4000-8000-000000000125', 'a2000000-0000-4000-8000-000000000102',
           'b1000000-0000-4000-8000-000000000106', 'ARM-FORGING', 1.000000, 'MAKE', 'FRG-42CrMo4', 3.800000, 0.070000
) v
WHERE NOT EXISTS (SELECT 1 FROM bom_node b WHERE b.bom_node_id = v.id)
  AND EXISTS (SELECT 1 FROM bom_node p WHERE p.bom_node_id = v.parent);

-- HORIZON product roots (>= 10)
INSERT INTO bom_node (
    bom_node_id, customer_id, program_id, parent_node_id, part_number, revision,
    quantity_per, uom, make_buy_flag, status, what_if_branch_flag, override_flag, created_at
)
SELECT v.id, @cust_horizon, 'a2000000-0000-4000-8000-000000000012', NULL, v.pn, 'A',
       1.000000, 'EA', 'MAKE', 'RELEASED', FALSE, FALSE, UTC_TIMESTAMP(6)
FROM (
    SELECT 'b1000000-0000-4000-8000-000000000201' AS id, 'HOR-BRAKE-CAL' AS pn
    UNION ALL SELECT 'b1000000-0000-4000-8000-000000000202', 'HOR-KNUCKLE'
    UNION ALL SELECT 'b1000000-0000-4000-8000-000000000203', 'HOR-MOUNT'
    UNION ALL SELECT 'b1000000-0000-4000-8000-000000000204', 'HOR-TRAY'
    UNION ALL SELECT 'b1000000-0000-4000-8000-000000000205', 'HOR-XMSN'
    UNION ALL SELECT 'b1000000-0000-4000-8000-000000000206', 'HOR-ARM'
    UNION ALL SELECT 'b1000000-0000-4000-8000-000000000207', 'HOR-SEAT'
    UNION ALL SELECT 'b1000000-0000-4000-8000-000000000208', 'HOR-CROSSBEAM'
    UNION ALL SELECT 'b1000000-0000-4000-8000-000000000209', 'HOR-MOTOR-HSG'
    UNION ALL SELECT 'b1000000-0000-4000-8000-000000000210', 'HOR-AL-BRKT'
) v
WHERE NOT EXISTS (SELECT 1 FROM bom_node b WHERE b.bom_node_id = v.id)
  AND EXISTS (SELECT 1 FROM vehicle_program p WHERE p.program_id = 'a2000000-0000-4000-8000-000000000012');

-- DEFAULT extra products to reach >= 10 (EV-CX chain already has some)
INSERT INTO bom_node (
    bom_node_id, customer_id, program_id, parent_node_id, part_number, revision,
    quantity_per, uom, make_buy_flag, status, what_if_branch_flag, override_flag, created_at
)
SELECT v.id, @cust_default, 'a2000000-0000-4000-8000-000000000001', NULL, v.pn, 'A',
       1.000000, 'EA', 'MAKE', 'RELEASED', FALSE, FALSE, UTC_TIMESTAMP(6)
FROM (
    SELECT 'b1000000-0000-4000-8000-000000000301' AS id, 'DEF-BRAKE-CAL' AS pn
    UNION ALL SELECT 'b1000000-0000-4000-8000-000000000302', 'DEF-KNUCKLE'
    UNION ALL SELECT 'b1000000-0000-4000-8000-000000000303', 'DEF-MOUNT'
    UNION ALL SELECT 'b1000000-0000-4000-8000-000000000304', 'DEF-TRAY'
    UNION ALL SELECT 'b1000000-0000-4000-8000-000000000305', 'DEF-XMSN'
    UNION ALL SELECT 'b1000000-0000-4000-8000-000000000306', 'DEF-ARM'
    UNION ALL SELECT 'b1000000-0000-4000-8000-000000000307', 'DEF-SEAT'
    UNION ALL SELECT 'b1000000-0000-4000-8000-000000000308', 'DEF-CROSSBEAM'
    UNION ALL SELECT 'b1000000-0000-4000-8000-000000000309', 'DEF-MOTOR-HSG'
    UNION ALL SELECT 'b1000000-0000-4000-8000-000000000310', 'DEF-AL-BRKT'
) v
WHERE NOT EXISTS (SELECT 1 FROM bom_node b WHERE b.bom_node_id = v.id);

-- ---------------------------------------------------------------------------
-- 4. SHOULD-COST builds (volume)
-- ---------------------------------------------------------------------------
INSERT INTO should_cost_build_up (
    build_up_id, customer_id, bom_node_id, scenario_label,
    material_cost, process_cost, tooling_cost, packaging_freight_cost,
    overhead_cost, sga_cost, profit_margin, total_cost,
    stale_inputs_flag, locked, calculation_basis, calculated_at, created_at
)
SELECT v.id, b.customer_id, v.bom, v.label,
       v.mat, v.proc, v.tool, v.pkg, v.oh, v.sga, v.profit, v.tot,
       FALSE, FALSE, CONCAT('Demo should-cost ', v.label), UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
FROM (
    SELECT 'e1000000-0000-4000-8000-000000000101' AS id, 'b1000000-0000-4000-8000-000000000121' AS bom, 'CAL-BODY baseline' AS label,
           6.200000 AS mat, 4.100000 AS proc, 0.850000 AS tool, 0.400000 AS pkg, 1.200000 AS oh, 0.600000 AS sga, 0.500000 AS profit, 13.850000 AS tot
    UNION ALL SELECT 'e1000000-0000-4000-8000-000000000102', 'b1000000-0000-4000-8000-000000000123', 'KNUCKLE-CAST baseline',
           13.950000, 8.500000, 1.200000, 0.800000, 2.100000, 1.000000, 0.900000, 28.450000
    UNION ALL SELECT 'e1000000-0000-4000-8000-000000000103', 'b1000000-0000-4000-8000-000000000124', 'MOUNT-BRACKET baseline',
           1.050000, 2.400000, 0.300000, 0.250000, 0.700000, 0.350000, 0.280000, 5.330000
    UNION ALL SELECT 'e1000000-0000-4000-8000-000000000104', 'b1000000-0000-4000-8000-000000000125', 'ARM-FORGING baseline',
           8.170000, 6.800000, 1.500000, 0.600000, 1.800000, 0.900000, 0.750000, 20.520000
    UNION ALL SELECT 'e1000000-0000-4000-8000-000000000105', 'b1000000-0000-4000-8000-000000000106', 'SUSP-CTRL-ARM assembly',
           12.000000, 9.500000, 2.000000, 0.900000, 2.500000, 1.200000, 1.000000, 29.100000
    UNION ALL SELECT 'e1000000-0000-4000-8000-000000000106', 'b1000000-0000-4000-8000-000000000109', 'EMOTOR-HOUSING',
           18.500000, 14.000000, 3.500000, 1.200000, 3.800000, 1.800000, 1.500000, 44.300000
    UNION ALL SELECT 'e1000000-0000-4000-8000-000000000107', 'b1000000-0000-4000-8000-000000000110', 'AL-STRUCT-BRKT',
           4.200000, 3.100000, 0.500000, 0.350000, 0.900000, 0.450000, 0.380000, 9.880000
    UNION ALL SELECT 'e1000000-0000-4000-8000-000000000108', 'b1000000-0000-4000-8000-000000000104', 'BATT-TRAY-ASM',
           45.000000, 22.000000, 8.000000, 3.500000, 7.000000, 3.500000, 3.000000, 92.000000
    UNION ALL SELECT 'e1000000-0000-4000-8000-000000000109', 'b1000000-0000-4000-8000-000000000108', 'IP-CROSS-BEAM',
           9.800000, 7.200000, 1.800000, 0.700000, 2.200000, 1.100000, 0.900000, 23.700000
    UNION ALL SELECT 'e1000000-0000-4000-8000-000000000110', 'b1000000-0000-4000-8000-000000000105', 'XMSN-HOUSING',
           28.000000, 18.500000, 5.000000, 2.000000, 5.500000, 2.800000, 2.200000, 64.000000
) v
JOIN bom_node b ON b.bom_node_id = v.bom
WHERE NOT EXISTS (SELECT 1 FROM should_cost_build_up s WHERE s.build_up_id = v.id);

-- Target costs (below / near / above scenarios via notes)
INSERT INTO target_cost_record (
    target_cost_id, customer_id, bom_node_id, program_id, target_amount, currency_code,
    effective_from, status, approval_status, notes, created_at
)
SELECT v.id, b.customer_id, v.bom, v.prog, v.amt, 'USD', '2026-01-01', 'ACTIVE', 'APPROVED', v.notes, UTC_TIMESTAMP(6)
FROM (
    SELECT 'e1000000-0000-4000-8000-000000000151' AS id, 'b1000000-0000-4000-8000-000000000121' AS bom,
           'a2000000-0000-4000-8000-000000000101' AS prog, 14.500000 AS amt, 'Near target (should ~13.85)' AS notes
    UNION ALL SELECT 'e1000000-0000-4000-8000-000000000152', 'b1000000-0000-4000-8000-000000000123',
           'a2000000-0000-4000-8000-000000000101', 25.000000, 'Above target (should ~28.45)'
    UNION ALL SELECT 'e1000000-0000-4000-8000-000000000153', 'b1000000-0000-4000-8000-000000000124',
           'a2000000-0000-4000-8000-000000000101', 6.500000, 'Below target (should ~5.33)'
    UNION ALL SELECT 'e1000000-0000-4000-8000-000000000154', 'b1000000-0000-4000-8000-000000000125',
           'a2000000-0000-4000-8000-000000000102', 19.000000, 'Near target'
    UNION ALL SELECT 'e1000000-0000-4000-8000-000000000155', 'b1000000-0000-4000-8000-000000000104',
           'a2000000-0000-4000-8000-000000000101', 100.000000, 'Below target (should ~92)'
) v
JOIN bom_node b ON b.bom_node_id = v.bom
WHERE NOT EXISTS (SELECT 1 FROM target_cost_record t WHERE t.target_cost_id = v.id);

-- ---------------------------------------------------------------------------
-- 5. SUPPLIERS (AURORA + HORIZON volume)
-- ---------------------------------------------------------------------------
INSERT INTO supplier_master (
    supplier_id, customer_id, supplier_code, supplier_name, country_code, currency_code, status, notes, created_at
)
SELECT v.id, @cust_aurora, v.code, v.name, v.cc, 'USD', CASE WHEN v.status = 'PENDING_APPROVAL' THEN 'ACTIVE' ELSE v.status END, v.notes, UTC_TIMESTAMP(6)
FROM (
    SELECT 'c1000000-0000-4000-8000-000000000101' AS id, 'AUR-METAL-01' AS code, 'Aurora Metal Works' AS name, 'US' AS cc, 'ACTIVE' AS status, 'Metal stamping' AS notes
    UNION ALL SELECT 'c1000000-0000-4000-8000-000000000102', 'AUR-PLAS-01', 'Aurora Plastics Inc', 'US', 'ACTIVE', 'Plastic injection'
    UNION ALL SELECT 'c1000000-0000-4000-8000-000000000103', 'AUR-FORG-01', 'ForgeTech Americas', 'US', 'ACTIVE', 'Forging'
    UNION ALL SELECT 'c1000000-0000-4000-8000-000000000104', 'AUR-CAST-01', 'CastPro Foundry', 'MX', 'ACTIVE', 'Casting'
    UNION ALL SELECT 'c1000000-0000-4000-8000-000000000105', 'AUR-MACH-01', 'Precision Machining LLC', 'US', 'ACTIVE', 'Machining'
    UNION ALL SELECT 'c1000000-0000-4000-8000-000000000106', 'AUR-TOOL-01', 'ToolCraft Dies', 'US', 'ACTIVE', 'Tooling'
    UNION ALL SELECT 'c1000000-0000-4000-8000-000000000107', 'AUR-ELEC-01', 'ElectroSense Components', 'CN', 'PENDING_APPROVAL', 'Electronics'
    UNION ALL SELECT 'c1000000-0000-4000-8000-000000000108', 'AUR-PACK-01', 'PackRight Logistics', 'US', 'ACTIVE', 'Packaging'
    UNION ALL SELECT 'c1000000-0000-4000-8000-000000000109', 'AUR-ALUM-01', 'AluForm Extrusions', 'CA', 'ACTIVE', 'Aluminium'
    UNION ALL SELECT 'c1000000-0000-4000-8000-000000000110', 'AUR-RUB-01', 'SealTech Rubber', 'IN', 'INACTIVE', 'Rubber seals'
) v
WHERE NOT EXISTS (SELECT 1 FROM supplier_master s WHERE s.supplier_code = v.code);

INSERT INTO supplier_master (
    supplier_id, customer_id, supplier_code, supplier_name, country_code, currency_code, status, notes, created_at
)
SELECT v.id, @cust_horizon, v.code, v.name, 'IN', 'INR', 'ACTIVE', 'Horizon supplier', UTC_TIMESTAMP(6)
FROM (
    SELECT 'c1000000-0000-4000-8000-000000000201' AS id, 'HOR-SUP-02' AS code, 'Horizon Stampings Pvt' AS name
    UNION ALL SELECT 'c1000000-0000-4000-8000-000000000202', 'HOR-SUP-03', 'Horizon Castings Ltd'
    UNION ALL SELECT 'c1000000-0000-4000-8000-000000000203', 'HOR-SUP-04', 'Horizon Plastics'
    UNION ALL SELECT 'c1000000-0000-4000-8000-000000000204', 'HOR-SUP-05', 'Horizon Machining'
    UNION ALL SELECT 'c1000000-0000-4000-8000-000000000205', 'HOR-SUP-06', 'Horizon Tooling'
    UNION ALL SELECT 'c1000000-0000-4000-8000-000000000206', 'HOR-SUP-07', 'Horizon Fasteners'
    UNION ALL SELECT 'c1000000-0000-4000-8000-000000000207', 'HOR-SUP-08', 'Horizon Rubber Works'
) v
WHERE NOT EXISTS (SELECT 1 FROM supplier_master s WHERE s.supplier_code = v.code);

-- ---------------------------------------------------------------------------
-- 6. AUDIT volume (>= 50 events cumulative with prior)
-- ---------------------------------------------------------------------------
INSERT INTO audit_log (audit_id, entity_type, entity_id, action, user_id, new_value, timestamp)
SELECT UUID(), v.etype, v.eid, v.action, 'f1000000-0000-4000-8000-000000000002', JSON_OBJECT('details', v.details), UTC_TIMESTAMP(6)
FROM (
    SELECT 'BOM_NODE' AS etype, 'b1000000-0000-4000-8000-000000000101' AS eid, 'CREATED' AS action, 'Demo: Brake caliper root created' AS details
    UNION ALL SELECT 'BOM_NODE', 'b1000000-0000-4000-8000-000000000102', 'CREATED', 'Demo: Steering knuckle root'
    UNION ALL SELECT 'BOM_NODE', 'b1000000-0000-4000-8000-000000000103', 'CREATED', 'Demo: Engine mount bracket'
    UNION ALL SELECT 'SHOULD_COST', 'e1000000-0000-4000-8000-000000000101', 'CALCULATED', 'Demo should-cost CAL-BODY'
    UNION ALL SELECT 'SHOULD_COST', 'e1000000-0000-4000-8000-000000000102', 'CALCULATED', 'Demo should-cost KNUCKLE'
    UNION ALL SELECT 'TARGET_COST', 'e1000000-0000-4000-8000-000000000151', 'APPROVED', 'Demo target approved'
    UNION ALL SELECT 'SUPPLIER', 'c1000000-0000-4000-8000-000000000101', 'CREATED', 'Demo supplier AUR-METAL-01'
    UNION ALL SELECT 'SUPPLIER', 'c1000000-0000-4000-8000-000000000107', 'STATUS_CHANGE', 'Pending approval'
    UNION ALL SELECT 'MATERIAL', 'a3000000-0000-4000-8000-000000000101', 'CREATED', 'Demo material STL-ST12'
    UNION ALL SELECT 'PROGRAM', 'a2000000-0000-4000-8000-000000000101', 'CREATED', 'EV Platform 2027'
    UNION ALL SELECT 'PROGRAM', 'a2000000-0000-4000-8000-000000000104', 'CREATED', 'Electric Powertrain'
    UNION ALL SELECT 'BOM_NODE', 'b1000000-0000-4000-8000-000000000104', 'CREATED', 'Battery tray'
    UNION ALL SELECT 'BOM_NODE', 'b1000000-0000-4000-8000-000000000109', 'CREATED', 'E-motor housing'
    UNION ALL SELECT 'SHOULD_COST', 'e1000000-0000-4000-8000-000000000108', 'CALCULATED', 'Battery tray should-cost'
    UNION ALL SELECT 'MATERIAL', 'a3000000-0000-4000-8000-000000000108', 'CREATED', 'PA66-GF30'
    UNION ALL SELECT 'SUPPLIER', 'c1000000-0000-4000-8000-000000000103', 'CREATED', 'ForgeTech'
    UNION ALL SELECT 'BOM_NODE', 'b1000000-0000-4000-8000-000000000201', 'CREATED', 'Horizon brake caliper'
    UNION ALL SELECT 'MATERIAL', 'a3000000-0000-4000-8000-000000000201', 'CREATED', 'Horizon CRS'
    UNION ALL SELECT 'SUPPLIER', 'c1000000-0000-4000-8000-000000000201', 'CREATED', 'Horizon Stampings'
    UNION ALL SELECT 'PROGRAM', 'a2000000-0000-4000-8000-000000000105', 'CREATED', 'Premium Sedan'
) v
WHERE NOT EXISTS (
    SELECT 1 FROM audit_log a
    WHERE a.entity_id = v.eid
      AND a.action = v.action
      AND JSON_UNQUOTE(JSON_EXTRACT(a.new_value, '$.details')) = v.details
);

-- End V168
