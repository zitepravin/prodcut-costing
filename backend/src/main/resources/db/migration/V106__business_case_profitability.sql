-- Program business-case / profitability scenarios (gap-report P0)
-- Complements should-cost; does NOT replace ERP P&L or Material Ledger.

CREATE TABLE IF NOT EXISTS business_case_scenario (
    scenario_id       CHAR(36)     NOT NULL PRIMARY KEY,
    program_id        CHAR(36)     NOT NULL,
    scenario_code     VARCHAR(40)  NOT NULL,
    scenario_name     VARCHAR(200) NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    currency_code     CHAR(3)      NOT NULL DEFAULT 'USD',
    unit_selling_price DECIMAL(18,6) NOT NULL DEFAULT 0,
    unit_should_cost  DECIMAL(18,6) NOT NULL DEFAULT 0,
    unit_variable_cost DECIMAL(18,6) NOT NULL DEFAULT 0,
    annual_volume     DECIMAL(18,6) NOT NULL DEFAULT 0,
    horizon_years     INT          NOT NULL DEFAULT 5,
    discount_rate_pct DECIMAL(9,6) NOT NULL DEFAULT 0.100000,
    initial_investment DECIMAL(18,6) NOT NULL DEFAULT 0,
    fixed_annual_cost DECIMAL(18,6) NOT NULL DEFAULT 0,
    contribution_margin_unit DECIMAL(18,6) NULL,
    annual_contribution DECIMAL(18,6) NULL,
    npv_amount        DECIMAL(18,6) NULL,
    payback_years     DECIMAL(18,6) NULL,
    calculation_basis VARCHAR(2000) NULL,
    calculated_at     TIMESTAMP(6) NULL,
    created_by        VARCHAR(36)  NULL,
    created_at        TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by        VARCHAR(36)  NULL,
    updated_at        TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_bc_program_code (program_id, scenario_code),
    KEY idx_bc_program (program_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
