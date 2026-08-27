-- P3-07b questionnaire packs + P3-07c tenant costing defaults

CREATE TABLE questionnaire_pack (
    pack_id       CHAR(36)      NOT NULL,
    pack_code     VARCHAR(50)   NOT NULL,
    pack_name     VARCHAR(200)  NOT NULL,
    purpose       VARCHAR(40)   NOT NULL DEFAULT 'EVALUATION',
    status        VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    version_no    INT           NOT NULL DEFAULT 1,
    description   VARCHAR(1000) NULL,
    created_by    VARCHAR(36)   NULL,
    created_at    TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by    VARCHAR(36)   NULL,
    updated_at    TIMESTAMP(6)  NULL,
    PRIMARY KEY (pack_id),
    UNIQUE KEY uk_qpack_code_ver (pack_code, version_no)
);

CREATE TABLE questionnaire_pack_item (
    item_id     CHAR(36) NOT NULL,
    pack_id     CHAR(36) NOT NULL,
    question_id CHAR(36) NOT NULL,
    sort_order  INT      NOT NULL DEFAULT 0,
    PRIMARY KEY (item_id),
    UNIQUE KEY uk_qpack_item (pack_id, question_id),
    CONSTRAINT fk_qpack_item_pack FOREIGN KEY (pack_id) REFERENCES questionnaire_pack (pack_id),
    CONSTRAINT fk_qpack_item_q FOREIGN KEY (question_id) REFERENCES vendor_question (question_id)
);

CREATE TABLE tenant_costing_defaults (
    defaults_id                 CHAR(36)       NOT NULL,
    customer_id                 CHAR(36)       NOT NULL,
    default_reporting_currency  CHAR(3)        NULL,
    default_scrap_percent       DECIMAL(9, 4)  NULL,
    default_overhead_percent    DECIMAL(9, 4)  NULL,
    -- fx_missing_policy and release_incomplete_policy are system-fixed (NOT_AVAILABLE / BLOCK)
    notes                       VARCHAR(1000)  NULL,
    created_by                  VARCHAR(36)    NULL,
    created_at                  TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by                  VARCHAR(36)    NULL,
    updated_at                  TIMESTAMP(6)   NULL,
    PRIMARY KEY (defaults_id),
    UNIQUE KEY uk_tcd_customer (customer_id)
);
