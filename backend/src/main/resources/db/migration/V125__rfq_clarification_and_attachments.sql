-- Seller clarification requests + RFQ attachments metadata (no binary in DB)

CREATE TABLE rfq_attachment (
    attachment_id   CHAR(36)       NOT NULL,
    rfq_id          CHAR(36)       NOT NULL,
    file_name       VARCHAR(255)   NOT NULL,
    content_type    VARCHAR(100)   NULL,
    storage_uri     VARCHAR(1000)  NOT NULL,
    uploaded_by     VARCHAR(36)    NULL,
    uploaded_at     TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    notes           VARCHAR(500)   NULL,
    PRIMARY KEY (attachment_id),
    KEY idx_rfq_att_rfq (rfq_id)
);

CREATE TABLE rfq_clarification_request (
    clarification_id CHAR(36)      NOT NULL,
    rfq_id           CHAR(36)      NOT NULL,
    supplier_id      CHAR(36)      NOT NULL,
    question_text    VARCHAR(2000) NOT NULL,
    status           VARCHAR(20)   NOT NULL DEFAULT 'OPEN',
    buyer_response   VARCHAR(2000) NULL,
    responded_by     VARCHAR(36)   NULL,
    responded_at     TIMESTAMP(6)  NULL,
    created_by       VARCHAR(36)   NULL,
    created_at       TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by       VARCHAR(36)   NULL,
    updated_at       TIMESTAMP(6)  NULL,
    PRIMARY KEY (clarification_id),
    KEY idx_rfq_clar_rfq (rfq_id),
    KEY idx_rfq_clar_supplier (supplier_id, status)
);
