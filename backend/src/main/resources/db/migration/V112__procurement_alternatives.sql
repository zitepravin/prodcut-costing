-- B6 Procurement Alternatives — first-class MAKE / BUY / SUBCONTRACT / EXTERNAL / MIXED set

CREATE TABLE IF NOT EXISTS procurement_alternative_set (
    set_id            CHAR(36)      NOT NULL PRIMARY KEY,
    bom_node_id       CHAR(36)      NOT NULL,
    program_id        CHAR(36)      NULL,
    label             VARCHAR(200)  NOT NULL,
    status            VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    -- DRAFT | EVALUATED | DECIDED
    recommendation_code VARCHAR(40) NULL,
    recommendation_summary VARCHAR(2000) NULL,
    decision_code     VARCHAR(40)   NULL,
    decision_reason   VARCHAR(1000) NULL,
    decided_by        VARCHAR(36)   NULL,
    decided_at        TIMESTAMP(6)  NULL,
    created_by        VARCHAR(36)   NULL,
    created_at        TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by        VARCHAR(36)   NULL,
    updated_at        TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    KEY idx_pas_bom (bom_node_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS procurement_alternative_line (
    line_id           CHAR(36)      NOT NULL PRIMARY KEY,
    set_id            CHAR(36)      NOT NULL,
    sequence_no       INT           NOT NULL,
    alternative_type  VARCHAR(40)   NOT NULL,
    -- MAKE | BUY | SUBCONTRACT | EXTERNAL_PROCESS | MIXED
    source_ref_type   VARCHAR(40)   NULL,
    source_ref_id     CHAR(36)      NULL,
    supplier_id       CHAR(36)      NULL,
    supplier_name     VARCHAR(200)  NULL,
    unit_cost         DECIMAL(18,6) NULL,
    currency_code     CHAR(3)       NULL,
    tco_unit          DECIMAL(18,6) NULL,
    tco_status        VARCHAR(20)   NOT NULL DEFAULT 'DATA_NOT_AVAILABLE',
    eligibility       VARCHAR(20)   NOT NULL DEFAULT 'UNKNOWN',
    rank_no           INT           NULL,
    data_status       VARCHAR(30)   NOT NULL DEFAULT 'OK',
    notes             VARCHAR(500)  NULL,
    CONSTRAINT fk_pal_set FOREIGN KEY (set_id) REFERENCES procurement_alternative_set(set_id),
    KEY idx_pal_set (set_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
