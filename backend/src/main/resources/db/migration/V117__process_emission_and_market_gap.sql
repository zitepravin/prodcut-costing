-- Market gap: process emission factors already fit emission_factor.category=PROCESS
-- Add optional default process factor config + benchmark analytics is API-only

CREATE TABLE IF NOT EXISTS process_emission_default (
    config_id           CHAR(36)      NOT NULL PRIMARY KEY,
    plant_code          VARCHAR(40)   NULL,
    process_category    VARCHAR(40)   NOT NULL DEFAULT 'DEFAULT',
    factor_id           CHAR(36)      NOT NULL,
    active              BOOLEAN       NOT NULL DEFAULT TRUE,
    notes               VARCHAR(500)  NULL,
    created_at          TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_ped_plant (plant_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
