-- P3-01 Purchase Request (purchase intent before ERP PO)
-- Module code PURCHASE already seeded in V32 (registry). Productization starts here.

CREATE TABLE purchase_request (
    purchase_request_id       CHAR(36)       NOT NULL,
    customer_id               CHAR(36)       NULL,
    program_id                CHAR(36)       NOT NULL,
    bom_node_id               CHAR(36)       NOT NULL,
    make_vs_buy_simulation_id CHAR(36)       NULL,
    status                    VARCHAR(20)    NOT NULL DEFAULT 'DRAFT',
    reporting_currency        CHAR(3)        NOT NULL,
    manual_override_reason    VARCHAR(1000)  NULL,
    decision_code             VARCHAR(20)    NULL,
    decision_reason           VARCHAR(1000)  NULL,
    decided_by                VARCHAR(36)    NULL,
    decided_at                TIMESTAMP(6)   NULL,
    released_at               TIMESTAMP(6)   NULL,
    external_po_reference     VARCHAR(100)   NULL,
    notes                     VARCHAR(1000)  NULL,
    created_by                VARCHAR(36)    NULL,
    created_at                TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by                VARCHAR(36)    NULL,
    updated_at                TIMESTAMP(6)   NULL,
    PRIMARY KEY (purchase_request_id),
    KEY idx_pr_bom (bom_node_id),
    KEY idx_pr_program_status (program_id, status),
    KEY idx_pr_mvb (make_vs_buy_simulation_id)
);

CREATE TABLE purchase_request_line (
    line_id                   CHAR(36)       NOT NULL,
    purchase_request_id       CHAR(36)       NOT NULL,
    line_no                   INT            NOT NULL,
    supplier_id               CHAR(36)       NOT NULL,
    supplier_quote_id         CHAR(36)       NULL,
    quantity                  DECIMAL(18, 6) NOT NULL,
    uom                       VARCHAR(20)    NOT NULL DEFAULT 'EA',
    unit_price                DECIMAL(18, 6) NULL,
    currency                  CHAR(3)        NULL,
    unit_price_reporting      DECIMAL(18, 6) NULL,
    fx_rate                   DECIMAL(18, 8) NULL,
    data_status               VARCHAR(20)    NOT NULL DEFAULT 'NOT_AVAILABLE',
    tco_unit                  DECIMAL(18, 6) NULL,
    tco_status                VARCHAR(20)    NOT NULL DEFAULT 'NOT_AVAILABLE',
    need_by_date              DATE           NULL,
    line_notes                VARCHAR(500)   NULL,
    created_by                VARCHAR(36)    NULL,
    created_at                TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by                VARCHAR(36)    NULL,
    updated_at                TIMESTAMP(6)   NULL,
    PRIMARY KEY (line_id),
    UNIQUE KEY uk_pr_line_no (purchase_request_id, line_no),
    CONSTRAINT fk_pr_line_header FOREIGN KEY (purchase_request_id)
        REFERENCES purchase_request (purchase_request_id)
);
