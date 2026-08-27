-- V92: Cost Component Structure + deep variance (P2-01)
CREATE TABLE cost_component_definition (
    component_id CHAR(36) NOT NULL,
    tenant_id CHAR(36) NULL,
    component_code VARCHAR(40) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    sequence_no INT NOT NULL,
    category VARCHAR(40) NOT NULL,
    maps_to_column VARCHAR(40) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (component_id),
    UNIQUE KEY uk_ccd_tenant_code (tenant_id, component_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE should_cost_quote_component (
    id CHAR(36) NOT NULL,
    build_up_id CHAR(36) NOT NULL,
    component_code VARCHAR(40) NOT NULL,
    quoted_amount DECIMAL(18,6) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    source_note VARCHAR(255) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_scqc_buildup (build_up_id),
    CONSTRAINT fk_scqc_buildup FOREIGN KEY (build_up_id) REFERENCES should_cost_build_up (build_up_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO cost_component_definition (component_id, tenant_id, component_code, display_name, sequence_no, category, maps_to_column, active, created_at, updated_at) VALUES
('ccd-sys-material-0000000000000001', NULL, 'MATERIAL', 'Material', 10, 'DIRECT', 'material_cost', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
('ccd-sys-process-00000000000000002', NULL, 'PROCESS', 'Process', 20, 'DIRECT', 'process_cost', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
('ccd-sys-tooling-00000000000000003', NULL, 'TOOLING', 'Tooling', 30, 'DIRECT', 'tooling_cost', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
('ccd-sys-packfreight-000000000004', NULL, 'PACKAGING_FREIGHT', 'Packaging & Freight', 40, 'DIRECT', 'packaging_freight_cost', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
('ccd-sys-overhead-000000000000005', NULL, 'OVERHEAD', 'Overhead', 50, 'OVERHEAD', 'overhead_cost', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
('ccd-sys-sga-00000000000000000006', NULL, 'SGA', 'SG&A', 60, 'COMMERCIAL', 'sga_cost', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
('ccd-sys-margin-00000000000000007', NULL, 'PROFIT_MARGIN', 'Profit Margin', 70, 'COMMERCIAL', 'profit_margin', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
('ccd-sys-total-000000000000000008', NULL, 'TOTAL', 'Total', 999, 'OTHER', 'total_cost', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));
