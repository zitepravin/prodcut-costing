-- A3 Cost Component Structure 2.0 + A5 itemization storage

ALTER TABLE cost_component_definition
    ADD COLUMN parent_component_code VARCHAR(40) NULL AFTER category,
    ADD COLUMN cost_behavior VARCHAR(20) NOT NULL DEFAULT 'VARIABLE' AFTER parent_component_code,
    ADD COLUMN hierarchy_level INT NOT NULL DEFAULT 1 AFTER cost_behavior,
    ADD KEY idx_ccd_parent (parent_component_code);

-- Hierarchical children under existing top-level components (system seed, tenant_id NULL)
INSERT INTO cost_component_definition
(component_id, tenant_id, component_code, display_name, sequence_no, category, parent_component_code, cost_behavior, hierarchy_level, maps_to_column, active, created_at, updated_at)
VALUES
('ccd-sys-mat-raw-000000000000001', NULL, 'RAW_MATERIAL', 'Raw Material', 11, 'DIRECT', 'MATERIAL', 'VARIABLE', 2, NULL, TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
('ccd-sys-mat-purch-000000000002', NULL, 'PURCHASED_PARTS', 'Purchased Parts', 12, 'DIRECT', 'MATERIAL', 'VARIABLE', 2, NULL, TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
('ccd-sys-mat-ext-00000000000003', NULL, 'EXTERNAL_PROCESSING', 'External Processing', 13, 'DIRECT', 'MATERIAL', 'VARIABLE', 2, NULL, TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
('ccd-sys-proc-labor-00000000004', NULL, 'DIRECT_LABOR', 'Direct Labor', 21, 'DIRECT', 'PROCESS', 'VARIABLE', 2, NULL, TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
('ccd-sys-proc-machine-000000005', NULL, 'MACHINE', 'Machine', 22, 'DIRECT', 'PROCESS', 'VARIABLE', 2, NULL, TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
('ccd-sys-proc-setup-00000000006', NULL, 'SETUP', 'Setup', 23, 'DIRECT', 'PROCESS', 'FIXED', 2, NULL, TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
('ccd-sys-oh-factory-00000000007', NULL, 'FACTORY_OH', 'Factory OH', 51, 'OVERHEAD', 'OVERHEAD', 'FIXED', 2, NULL, TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
('ccd-sys-oh-admin-000000000008', NULL, 'ADMIN_OH', 'Administration OH', 52, 'OVERHEAD', 'OVERHEAD', 'FIXED', 2, NULL, TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
('ccd-sys-oh-logistics-00000009', NULL, 'LOGISTICS_OH', 'Logistics OH', 53, 'OVERHEAD', 'OVERHEAD', 'VARIABLE', 2, NULL, TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE display_name = VALUES(display_name);

-- A5: cost estimate itemization lines (traceable Qty × Rate × Source × Component → Amount)
CREATE TABLE IF NOT EXISTS cost_estimate_item (
    item_id            CHAR(36)      NOT NULL PRIMARY KEY,
    estimate_ref_type  VARCHAR(40)   NOT NULL,
    estimate_ref_id    CHAR(36)      NOT NULL,
    bom_node_id        CHAR(36)      NULL,
    line_no            INT           NOT NULL,
    component_code     VARCHAR(40)   NOT NULL,
    description        VARCHAR(300)  NULL,
    quantity           DECIMAL(18,6) NOT NULL DEFAULT 1.000000,
    unit_of_measure    VARCHAR(30)   NULL,
    rate               DECIMAL(18,6) NOT NULL DEFAULT 0.000000,
    amount             DECIMAL(18,6) NOT NULL DEFAULT 0.000000,
    currency_code      CHAR(3)       NOT NULL DEFAULT 'USD',
    cost_behavior      VARCHAR(20)   NOT NULL DEFAULT 'VARIABLE',
    price_source       VARCHAR(80)   NULL,
    calculation_rule   VARCHAR(500)  NULL,
    created_at         TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_cei_ref (estimate_ref_type, estimate_ref_id),
    KEY idx_cei_bom (bom_node_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
