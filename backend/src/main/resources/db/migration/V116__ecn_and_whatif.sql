-- E3 ECN cost impact + E4 what-if simulation runs

CREATE TABLE IF NOT EXISTS ecn_cost_impact (
    impact_id           CHAR(36)      NOT NULL PRIMARY KEY,
    ecn_number           VARCHAR(60)   NOT NULL,
    ecn_title            VARCHAR(200)  NULL,
    program_id          CHAR(36)      NULL,
    bom_node_id         CHAR(36)      NOT NULL,
    baseline_build_up_id CHAR(36)     NULL,
    proposed_build_up_id CHAR(36)     NULL,
    baseline_unit_cost  DECIMAL(18,6) NULL,
    proposed_unit_cost  DECIMAL(18,6) NULL,
    unit_delta          DECIMAL(18,6) NULL,
    annual_volume       DECIMAL(18,6) NULL,
    annual_program_delta DECIMAL(18,6) NULL,
    currency_code       CHAR(3)       NOT NULL DEFAULT 'USD',
    status              VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    -- DRAFT | ANALYZED | APPROVED | REJECTED
    impact_summary      VARCHAR(2000) NULL,
    notes               VARCHAR(1000) NULL,
    created_by          VARCHAR(36)   NULL,
    created_at          TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    KEY idx_ecn_num (ecn_number),
    KEY idx_ecn_bom (bom_node_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS whatif_simulation (
    simulation_id       CHAR(36)      NOT NULL PRIMARY KEY,
    bom_node_id         CHAR(36)      NOT NULL,
    program_id          CHAR(36)      NULL,
    label               VARCHAR(200)  NOT NULL,
    baseline_unit_cost  DECIMAL(18,6) NULL,
    baseline_currency   CHAR(3)       NULL,
    material_pct_change DECIMAL(9,6)  NULL,
    volume_pct_change   DECIMAL(9,6)  NULL,
    fx_pct_change       DECIMAL(9,6)  NULL,
    process_pct_change  DECIMAL(9,6)  NULL,
    tooling_pct_change  DECIMAL(9,6)  NULL,
    simulated_unit_cost DECIMAL(18,6) NULL,
    unit_delta          DECIMAL(18,6) NULL,
    annual_volume       DECIMAL(18,6) NULL,
    annual_delta        DECIMAL(18,6) NULL,
    calculation_basis   VARCHAR(2000) NULL,
    status              VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    created_at          TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_wif_bom (bom_node_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
