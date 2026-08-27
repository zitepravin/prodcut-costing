-- B4 Costing Scenario registry + B5 Additive cost lines

CREATE TABLE IF NOT EXISTS costing_scenario (
    scenario_id         CHAR(36)      NOT NULL PRIMARY KEY,
    bom_node_id         CHAR(36)      NOT NULL,
    program_id          CHAR(36)      NULL,
    scenario_code       VARCHAR(40)   NOT NULL,
    scenario_name       VARCHAR(200)  NOT NULL,
    scenario_type       VARCHAR(40)   NOT NULL,
    -- BASE | SUPPLIER_A | SUPPLIER_B | MAKE | BUY | MIXED | ENGINEERING_CHANGE
    -- | VOLUME_CHANGE | PLANT_CHANGE | MATERIAL_CHANGE | CURRENCY_CHANGE | CUSTOM
    status              VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    -- DRAFT | ACTIVE | APPROVED | SUPERSEDED
    parent_scenario_id  CHAR(36)      NULL,
    should_cost_build_up_id CHAR(36)  NULL,
    costing_variant_id  CHAR(36)      NULL,
    notes               VARCHAR(1000) NULL,
    locked              BOOLEAN       NOT NULL DEFAULT FALSE,
    created_by          VARCHAR(36)   NULL,
    created_at          TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by          VARCHAR(36)   NULL,
    updated_at          TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_cs_node_code (bom_node_id, scenario_code),
    KEY idx_cs_type (scenario_type),
    KEY idx_cs_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS additive_cost_line (
    line_id             CHAR(36)      NOT NULL PRIMARY KEY,
    scenario_id         CHAR(36)      NOT NULL,
    line_code           VARCHAR(40)   NOT NULL,
    line_name           VARCHAR(200)  NOT NULL,
    additive_type       VARCHAR(40)   NOT NULL,
    -- PROTOTYPE | LAUNCH_PREMIUM | SPECIAL_TOOLING | ONE_TIME_ENGINEERING
    -- | SPECIAL_FREIGHT | CUSTOMER_SPECIFIC | OTHER
    amount              DECIMAL(18,6) NOT NULL,
    currency_code       CHAR(3)       NOT NULL DEFAULT 'USD',
    amortized           BOOLEAN       NOT NULL DEFAULT FALSE,
    amortization_volume DECIMAL(18,6) NULL,
    amount_per_unit     DECIMAL(18,6) NULL,
    notes               VARCHAR(500)  NULL,
    created_at          TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_acl_scenario FOREIGN KEY (scenario_id) REFERENCES costing_scenario(scenario_id),
    UNIQUE KEY uk_acl_scenario_code (scenario_id, line_code),
    KEY idx_acl_type (additive_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
