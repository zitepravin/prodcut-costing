-- Wave D: Purchase Order document (independent PURCHASE module)
-- Does not post to SAP MM. Human-only approve/issue. Optional link to PR / RFQ.

CREATE TABLE purchase_order (
    purchase_order_id       CHAR(36)       NOT NULL,
    po_number               VARCHAR(40)    NULL,
    customer_id             CHAR(36)       NOT NULL,
    purchase_request_id     CHAR(36)       NULL,
    linked_rfq_id           CHAR(36)       NULL,
    supplier_id             CHAR(36)       NOT NULL,
    plant_code              VARCHAR(40)    NULL,
    buyer_user_id           VARCHAR(36)    NULL,
    currency_code           CHAR(3)        NOT NULL,
    payment_terms           VARCHAR(80)    NULL,
    incoterms               VARCHAR(40)    NULL,
    delivery_terms          VARCHAR(200)   NULL,
    order_date              DATE           NULL,
    required_delivery_date  DATE           NULL,
    status                  VARCHAR(20)    NOT NULL DEFAULT 'DRAFT',
    decision_reason         VARCHAR(1000)  NULL,
    decided_by              VARCHAR(36)    NULL,
    decided_at              TIMESTAMP(6)   NULL,
    issued_at               TIMESTAMP(6)   NULL,
    issued_by               VARCHAR(36)    NULL,
    external_po_reference   VARCHAR(100)   NULL,
    notes                   VARCHAR(1000)  NULL,
    created_by              VARCHAR(36)    NULL,
    created_at              TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by              VARCHAR(36)    NULL,
    updated_at              TIMESTAMP(6)   NULL,
    PRIMARY KEY (purchase_order_id),
    UNIQUE KEY uk_po_number_customer (customer_id, po_number),
    KEY idx_po_customer_status (customer_id, status),
    KEY idx_po_supplier (supplier_id),
    KEY idx_po_pr (purchase_request_id),
    KEY idx_po_rfq (linked_rfq_id)
);

CREATE TABLE purchase_order_line (
    line_id                 CHAR(36)       NOT NULL,
    purchase_order_id       CHAR(36)       NOT NULL,
    line_no                 INT            NOT NULL,
    material_code           VARCHAR(80)    NULL,
    material_description    VARCHAR(255)   NULL,
    quantity                DECIMAL(18, 6) NOT NULL,
    uom                     VARCHAR(20)    NOT NULL DEFAULT 'EA',
    unit_price              DECIMAL(18, 6) NULL,
    currency_code           CHAR(3)        NULL,
    tax_amount              DECIMAL(18, 6) NULL,
    discount_amount         DECIMAL(18, 6) NULL,
    delivery_date           DATE           NULL,
    plant_code              VARCHAR(40)    NULL,
    storage_location        VARCHAR(40)    NULL,
    data_status             VARCHAR(20)    NOT NULL DEFAULT 'NOT_AVAILABLE',
    line_notes              VARCHAR(500)   NULL,
    created_by              VARCHAR(36)    NULL,
    created_at              TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by              VARCHAR(36)    NULL,
    updated_at              TIMESTAMP(6)   NULL,
    PRIMARY KEY (line_id),
    UNIQUE KEY uk_po_line_no (purchase_order_id, line_no),
    CONSTRAINT fk_po_line_header FOREIGN KEY (purchase_order_id)
        REFERENCES purchase_order (purchase_order_id)
);

-- Status vocabulary (application-enforced):
-- DRAFT, SUBMITTED, APPROVAL, APPROVED, ISSUED, REJECTED, REWORK, CANCELLED
