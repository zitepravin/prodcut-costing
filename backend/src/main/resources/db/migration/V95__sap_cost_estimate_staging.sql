-- SAP CO Product Cost Planning inbound staging (P2 SAP integration v1)
CREATE TABLE sap_cost_estimate_staging (
    staging_id          CHAR(36)       NOT NULL,
    tenant_hint         VARCHAR(64)    NULL,
    source_system       VARCHAR(80)    NOT NULL DEFAULT 'SAP',
    sap_material        VARCHAR(40)    NOT NULL,
    sap_plant           VARCHAR(10)    NOT NULL,
    costing_variant     VARCHAR(20)    NULL,
    costing_version     VARCHAR(20)    NULL,
    estimate_date       DATE           NULL,
    currency_code       CHAR(3)        NULL,
    total_cost          DECIMAL(18, 6) NULL,
    status              VARCHAR(20)    NOT NULL DEFAULT 'LANDED',
    should_cost_build_up_id CHAR(36)   NULL,
    bom_node_id         CHAR(36)       NULL,
    error_message       VARCHAR(1000)  NULL,
    raw_payload         TEXT           NULL,
    received_at         TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    compared_at         TIMESTAMP(6)   NULL,
    created_by          VARCHAR(36)    NULL,
    created_at          TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6)   NULL,
    PRIMARY KEY (staging_id),
    KEY idx_sap_ce_material_plant (sap_material, sap_plant),
    KEY idx_sap_ce_status (status)
);

CREATE TABLE sap_cost_estimate_component_staging (
    staging_id          CHAR(36)       NOT NULL,
    component_seq       INT            NOT NULL,
    sap_component_code  VARCHAR(40)    NOT NULL,
    sap_component_name  VARCHAR(120)   NULL,
    amount              DECIMAL(18, 6) NOT NULL,
    mapped_component_code VARCHAR(40)  NULL,
    PRIMARY KEY (staging_id, component_seq),
    CONSTRAINT fk_sap_cec_staging FOREIGN KEY (staging_id)
        REFERENCES sap_cost_estimate_staging (staging_id)
);

CREATE TABLE sap_component_mapping (
    mapping_id          CHAR(36)       NOT NULL,
    sap_component_code  VARCHAR(40)    NOT NULL,
    our_component_code  VARCHAR(40)    NOT NULL,
    notes               VARCHAR(255)   NULL,
    active_flag         TINYINT(1)     NOT NULL DEFAULT 1,
    created_at          TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6)   NULL,
    PRIMARY KEY (mapping_id),
    UNIQUE KEY uq_sap_comp_map (sap_component_code)
);
