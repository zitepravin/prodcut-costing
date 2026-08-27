-- P3-11a: emission factor master + material default link + should-cost material CO2e snapshot

CREATE TABLE emission_factor (
    factor_id          CHAR(36)       NOT NULL,
    factor_code        VARCHAR(50)    NOT NULL,
    factor_name        VARCHAR(200)   NOT NULL,
    category           VARCHAR(20)    NOT NULL DEFAULT 'MATERIAL',
    co2e_value         DECIMAL(18, 6) NOT NULL,
    unit               VARCHAR(40)    NOT NULL DEFAULT 'KG_CO2E_PER_KG',
    source_type        VARCHAR(30)    NOT NULL DEFAULT 'MANUAL',
    source_reference   VARCHAR(500)   NULL,
    geography          VARCHAR(40)    NULL,
    version_no         INT            NOT NULL DEFAULT 1,
    status             VARCHAR(20)    NOT NULL DEFAULT 'DRAFT',
    effective_from     DATE           NULL,
    effective_to       DATE           NULL,
    created_by         VARCHAR(36)    NULL,
    created_at         TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by         VARCHAR(36)    NULL,
    updated_at         TIMESTAMP(6)   NULL,
    PRIMARY KEY (factor_id),
    UNIQUE KEY uk_emission_factor_code_ver (factor_code, version_no)
);

ALTER TABLE material_master
    ADD COLUMN default_emission_factor_id CHAR(36) NULL;

CREATE TABLE should_cost_emissions_snapshot (
    snapshot_id            CHAR(36)       NOT NULL,
    build_up_id            CHAR(36)       NOT NULL,
    bom_node_id            CHAR(36)       NOT NULL,
    material_co2e_kg       DECIMAL(18, 6) NULL,
    process_co2e_kg        DECIMAL(18, 6) NULL,
    total_co2e_kg          DECIMAL(18, 6) NULL,
    emissions_status       VARCHAR(20)    NOT NULL DEFAULT 'NOT_AVAILABLE',
    factor_id              CHAR(36)       NULL,
    factor_code            VARCHAR(50)    NULL,
    factor_version_no      INT            NULL,
    quantity_kg            DECIMAL(18, 6) NULL,
    calculation_notes      VARCHAR(2000)  NULL,
    calculated_at          TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (snapshot_id),
    UNIQUE KEY uk_sc_emis_build (build_up_id),
    KEY idx_sc_emis_bom (bom_node_id)
);
