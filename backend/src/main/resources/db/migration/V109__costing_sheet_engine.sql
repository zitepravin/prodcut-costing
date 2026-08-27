-- A6 Overhead / Costing Sheet Engine

CREATE TABLE IF NOT EXISTS costing_sheet (
    sheet_id          CHAR(36)      NOT NULL PRIMARY KEY,
    sheet_code        VARCHAR(40)   NOT NULL,
    sheet_name        VARCHAR(200)  NOT NULL,
    status            VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    plant_code        VARCHAR(40)   NULL,
    currency_code     CHAR(3)       NOT NULL DEFAULT 'USD',
    valid_from        DATE          NULL,
    valid_to          DATE          NULL,
    description       VARCHAR(1000) NULL,
    created_by        VARCHAR(36)   NULL,
    created_at        TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by        VARCHAR(36)   NULL,
    updated_at        TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_costing_sheet_code (sheet_code),
    KEY idx_costing_sheet_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS costing_sheet_rule (
    rule_id           CHAR(36)      NOT NULL PRIMARY KEY,
    sheet_id          CHAR(36)      NOT NULL,
    sequence_no       INT           NOT NULL,
    rule_code         VARCHAR(40)   NOT NULL,
    rule_name         VARCHAR(200)  NOT NULL,
    rule_type         VARCHAR(30)   NOT NULL,
    -- PERCENT_OF_BASE | FIXED_AMOUNT
    base_code         VARCHAR(40)   NULL,
    -- MATERIAL | PROCESS | TOOLING | PACKAGING | SUBTOTAL | TOTAL_SO_FAR
    percent_rate      DECIMAL(18,8) NULL,
    fixed_amount      DECIMAL(18,6) NULL,
    component_code    VARCHAR(40)   NOT NULL,
    cost_behavior     VARCHAR(20)   NOT NULL DEFAULT 'FIXED',
    active            BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_csr_sheet FOREIGN KEY (sheet_id) REFERENCES costing_sheet(sheet_id),
    UNIQUE KEY uk_csr_sheet_seq (sheet_id, sequence_no),
    UNIQUE KEY uk_csr_sheet_code (sheet_id, rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Link optional sheet on costing variant (column may already be present as costing_sheet_code)
