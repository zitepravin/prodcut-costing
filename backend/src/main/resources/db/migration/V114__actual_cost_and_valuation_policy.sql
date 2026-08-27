-- C2 Actual cost (import-only) + C4 valuation currency policy

CREATE TABLE IF NOT EXISTS actual_cost_record (
    actual_cost_id      CHAR(36)      NOT NULL PRIMARY KEY,
    bom_node_id         CHAR(36)      NOT NULL,
    program_id          CHAR(36)      NULL,
    source_system       VARCHAR(40)   NOT NULL DEFAULT 'IMPORT',
    -- IMPORT | SAP_STAGING | ERP_STAGING
    source_ref          VARCHAR(100)  NULL,
    period_code         VARCHAR(20)   NULL,
    quantity            DECIMAL(18,6) NULL,
    unit_cost           DECIMAL(18,6) NOT NULL,
    total_cost          DECIMAL(18,6) NULL,
    currency_code       CHAR(3)       NOT NULL DEFAULT 'USD',
    material_cost       DECIMAL(18,6) NULL,
    process_cost        DECIMAL(18,6) NULL,
    overhead_cost       DECIMAL(18,6) NULL,
    other_cost          DECIMAL(18,6) NULL,
    posting_date        DATE          NULL,
    imported_at         TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    notes               VARCHAR(500)  NULL,
    created_by          VARCHAR(36)   NULL,
    created_at          TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_acr_bom (bom_node_id),
    KEY idx_acr_period (period_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS valuation_currency_policy (
    policy_id           CHAR(36)      NOT NULL PRIMARY KEY,
    policy_code         VARCHAR(40)   NOT NULL,
    policy_name         VARCHAR(200)  NOT NULL,
    company_currency    CHAR(3)       NOT NULL,
    group_currency      CHAR(3)       NULL,
    transaction_currency CHAR(3)      NULL,
    costing_currency    CHAR(3)       NOT NULL,
    valuation_date_policy VARCHAR(40) NOT NULL DEFAULT 'COSTING_DATE',
    -- COSTING_DATE | DOCUMENT_DATE | TODAY | FIXED_DATE
    fixed_valuation_date DATE         NULL,
    active              BOOLEAN       NOT NULL DEFAULT TRUE,
    notes               VARCHAR(500)  NULL,
    created_at          TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_vcp_code (policy_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
