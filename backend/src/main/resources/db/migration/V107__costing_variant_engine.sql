-- A1: Costing Variant Engine — reusable rules for cost estimates (SAP-style control object).
-- Does not replace should-cost engine; variants parameterize how estimates are interpreted.

CREATE TABLE IF NOT EXISTS costing_variant (
    variant_id              CHAR(36)      NOT NULL PRIMARY KEY,
    variant_code            VARCHAR(40)   NOT NULL,
    variant_name            VARCHAR(200)  NOT NULL,
    status                  VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    valuation_strategy      VARCHAR(40)   NOT NULL DEFAULT 'STANDARD',
    material_price_source   VARCHAR(40)   NOT NULL DEFAULT 'MATERIAL_MASTER',
    activity_price_source   VARCHAR(40)   NOT NULL DEFAULT 'MACHINE_LABOR_MASTER',
    quantity_structure_source VARCHAR(40) NOT NULL DEFAULT 'BOM_AND_ROUTING',
    currency_code           CHAR(3)       NOT NULL DEFAULT 'USD',
    lot_size                DECIMAL(18,6) NOT NULL DEFAULT 1.000000,
    scrap_pct               DECIMAL(9,6)  NOT NULL DEFAULT 0.000000,
    yield_pct               DECIMAL(9,6)  NOT NULL DEFAULT 1.000000,
    overhead_pool_code      VARCHAR(40)   NULL,
    cost_component_profile  VARCHAR(40)   NULL,
    costing_sheet_code      VARCHAR(40)   NULL,
    valid_from              DATE          NULL,
    valid_to                DATE          NULL,
    description             VARCHAR(1000) NULL,
    created_by              VARCHAR(36)   NULL,
    created_at              TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by              VARCHAR(36)   NULL,
    updated_at              TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_costing_variant_code (variant_code),
    KEY idx_costing_variant_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Optional link from should-cost build-up to the variant used
ALTER TABLE should_cost_build_up
    ADD COLUMN costing_variant_id CHAR(36) NULL AFTER program_id,
    ADD KEY idx_scbu_variant (costing_variant_id);
