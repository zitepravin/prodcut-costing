-- P3-05: Read-only order tracking after RELEASED purchase request (CPI inbound; no SAP writes)

CREATE TABLE purchase_order_tracking (
    tracking_id               CHAR(36)       NOT NULL,
    purchase_request_id       CHAR(36)       NOT NULL,
    purchase_request_line_id  CHAR(36)       NULL,
    external_po_reference     VARCHAR(100)   NULL,
    tracking_status           VARCHAR(30)    NOT NULL DEFAULT 'UNKNOWN',
    asn_date                  DATE           NULL,
    qty_shipped               DECIMAL(18, 6) NULL,
    qty_ordered               DECIMAL(18, 6) NULL,
    uom                       VARCHAR(20)    NULL,
    source_system             VARCHAR(30)    NOT NULL DEFAULT 'CPI',
    source_event_id           VARCHAR(100)   NULL,
    data_status               VARCHAR(20)    NOT NULL DEFAULT 'NOT_AVAILABLE',
    human_corrected           TINYINT(1)     NOT NULL DEFAULT 0,
    correction_reason         VARCHAR(1000)  NULL,
    corrected_by              VARCHAR(36)    NULL,
    corrected_at              TIMESTAMP(6)   NULL,
    notes                     VARCHAR(1000)  NULL,
    created_by                VARCHAR(36)    NULL,
    created_at                TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by                VARCHAR(36)    NULL,
    updated_at                TIMESTAMP(6)   NULL,
    PRIMARY KEY (tracking_id),
    KEY idx_pot_pr (purchase_request_id),
    KEY idx_pot_ext (external_po_reference),
    KEY idx_pot_source_event (source_system, source_event_id),
    CONSTRAINT fk_pot_pr FOREIGN KEY (purchase_request_id)
        REFERENCES purchase_request (purchase_request_id)
);
