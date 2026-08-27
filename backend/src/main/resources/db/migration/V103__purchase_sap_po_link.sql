-- P3-09a: CPI callback stores external SAP PO number (inbound only; no SAP write)

ALTER TABLE purchase_request
    ADD COLUMN po_link_status      VARCHAR(20)  NOT NULL DEFAULT 'NOT_LINKED',
    ADD COLUMN external_po_system  VARCHAR(30)  NULL,
    ADD COLUMN po_linked_at        TIMESTAMP(6) NULL,
    ADD COLUMN po_linked_source    VARCHAR(30)  NULL,
    ADD COLUMN po_link_event_id    VARCHAR(100) NULL,
    ADD COLUMN po_link_notes       VARCHAR(1000) NULL;

CREATE UNIQUE INDEX uk_pr_po_link_event
    ON purchase_request (po_link_event_id);

CREATE TABLE purchase_po_link_event (
    event_id              CHAR(36)       NOT NULL,
    purchase_request_id   CHAR(36)       NOT NULL,
    source_event_id       VARCHAR(100)   NOT NULL,
    external_po_number    VARCHAR(100)   NOT NULL,
    external_po_system    VARCHAR(30)    NOT NULL DEFAULT 'SAP_MM',
    status                VARCHAR(20)    NOT NULL,
    notes                 VARCHAR(1000)  NULL,
    received_at           TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    received_by           VARCHAR(36)    NULL,
    PRIMARY KEY (event_id),
    UNIQUE KEY uk_po_link_source (source_event_id),
    KEY idx_po_link_pr (purchase_request_id),
    CONSTRAINT fk_po_link_pr FOREIGN KEY (purchase_request_id)
        REFERENCES purchase_request (purchase_request_id)
);
