-- P2-03 Mixed Costing / Multiple Procurement Alternatives
CREATE TABLE mixed_cost_version (
    mixed_cost_id       CHAR(36)       NOT NULL,
    program_id          CHAR(36)       NOT NULL,
    bom_node_id         CHAR(36)       NOT NULL,
    version_label       VARCHAR(120)   NOT NULL,
    status              VARCHAR(20)    NOT NULL DEFAULT 'DRAFT',
    reporting_currency  CHAR(3)        NULL,
    blended_unit_total  DECIMAL(18, 6) NULL,
    blended_status      VARCHAR(20)    NULL,
    ratio_sum_check     DECIMAL(10, 4) NULL,
    notes               VARCHAR(1000)  NULL,
    calculated_at       TIMESTAMP(6)   NULL,
    frozen_at           TIMESTAMP(6)   NULL,
    created_by          VARCHAR(36)    NULL,
    created_at          TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by          VARCHAR(36)    NULL,
    updated_at          TIMESTAMP(6)   NULL,
    PRIMARY KEY (mixed_cost_id),
    KEY idx_mcv_bom_node (bom_node_id),
    KEY idx_mcv_program_status (program_id, status),
    KEY idx_mcv_bom_status (bom_node_id, status)
);

CREATE TABLE mixed_cost_leg (
    mixed_cost_id       CHAR(36)       NOT NULL,
    leg_seq             INT            NOT NULL,
    alternative_type    VARCHAR(40)    NOT NULL,
    source_ref_id       CHAR(36)       NOT NULL,
    supplier_id         CHAR(36)       NULL,
    mix_ratio           DECIMAL(10, 4) NOT NULL,
    unit_cost_snapshot  DECIMAL(18, 6) NULL,
    currency_snapshot   CHAR(3)        NULL,
    eligibility_status  VARCHAR(20)    NOT NULL DEFAULT 'UNKNOWN',
    eligibility_notes   VARCHAR(500)   NULL,
    data_status         VARCHAR(20)    NOT NULL DEFAULT 'NOT_AVAILABLE',
    PRIMARY KEY (mixed_cost_id, leg_seq),
    CONSTRAINT fk_mcl_version FOREIGN KEY (mixed_cost_id) REFERENCES mixed_cost_version (mixed_cost_id)
);
