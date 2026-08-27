-- V155: Optional FTA / Trade Agreement module (platform-entitled)
-- Does NOT alter normal import duty calculation tables.

INSERT INTO feature_catalog (feature_id, feature_code, feature_name, module_code, description, active)
VALUES
  ('feat-fta-trade-agreement', 'FTA_TRADE_AGREEMENT', 'FTA / Trade Agreement', 'PRODUCT_COSTING',
   'Optional preferential tariff and rules of origin (requires IMPORT_COSTING)', 1)
ON DUPLICATE KEY UPDATE feature_name = VALUES(feature_name);

CREATE TABLE IF NOT EXISTS trade_agreement (
    agreement_id        CHAR(36)        NOT NULL,
    agreement_code      VARCHAR(40)     NOT NULL,
    agreement_name      VARCHAR(200)    NOT NULL,
    agreement_type      VARCHAR(40)     NULL,
    region              VARCHAR(40)     NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
    signed_date         DATE            NULL,
    effective_date      DATE            NULL,
    expiry_date         DATE            NULL,
    review_date         DATE            NULL,
    version_no          INT             NOT NULL DEFAULT 1,
    source              VARCHAR(200)    NULL,
    official_reference  VARCHAR(500)    NULL,
    enabled             TINYINT(1)      NOT NULL DEFAULT 1,
    platform_scope      TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '1=platform master readable by entitled customers',
    last_verified       DATE            NULL,
    approval_status     VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
    created_at          TIMESTAMP(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          TIMESTAMP(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_by          VARCHAR(64)     NULL,
    approved_by         VARCHAR(64)     NULL,
    PRIMARY KEY (agreement_id),
    UNIQUE KEY uq_ta_code_version (agreement_code, version_no),
    KEY idx_ta_status (status, enabled),
    KEY idx_ta_effective (effective_date, expiry_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS trade_agreement_partner (
    partner_id          CHAR(36)        NOT NULL,
    agreement_id        CHAR(36)        NOT NULL,
    country_code        CHAR(2)         NOT NULL,
    role_code           VARCHAR(20)     NOT NULL DEFAULT 'PARTY' COMMENT 'PARTY,ORIGIN_ELIGIBLE,CUMULATION',
    PRIMARY KEY (partner_id),
    UNIQUE KEY uq_tap (agreement_id, country_code, role_code),
    KEY idx_tap_country (country_code),
    CONSTRAINT fk_tap_agreement FOREIGN KEY (agreement_id) REFERENCES trade_agreement (agreement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS tariff_concession (
    concession_id       CHAR(36)        NOT NULL,
    agreement_id        CHAR(36)        NOT NULL,
    hs_code             VARCHAR(32)     NOT NULL,
    origin_country      CHAR(2)         NULL COMMENT 'NULL = all partners',
    destination_country CHAR(2)         NOT NULL,
    normal_rate         DECIMAL(18,8)   NULL,
    preferential_rate   DECIMAL(18,8)   NULL,
    rate_type           VARCHAR(20)     NOT NULL DEFAULT 'PERCENTAGE',
    specific_amount     DECIMAL(18,6)   NULL,
    quota               DECIMAL(18,6)   NULL,
    quota_unit          VARCHAR(20)     NULL,
    exclusion_flag      TINYINT(1)      NOT NULL DEFAULT 0,
    effective_from      DATE            NOT NULL,
    effective_to        DATE            NULL,
    source              VARCHAR(200)    NULL,
    version_no          INT             NOT NULL DEFAULT 1,
    approval_status     VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
    active              TINYINT(1)      NOT NULL DEFAULT 1,
    created_at          TIMESTAMP(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          TIMESTAMP(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (concession_id),
    KEY idx_tc_agreement_hs (agreement_id, hs_code),
    KEY idx_tc_dest (destination_country),
    KEY idx_tc_effective (effective_from, effective_to),
    CONSTRAINT fk_tc_agreement FOREIGN KEY (agreement_id) REFERENCES trade_agreement (agreement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS origin_rule (
    origin_rule_id      CHAR(36)        NOT NULL,
    agreement_id        CHAR(36)        NOT NULL,
    hs_code             VARCHAR(32)     NOT NULL,
    rule_type           VARCHAR(40)     NOT NULL COMMENT 'WHOLLY_OBTAINED,CTC,CTH,CTSH,RVC,SPECIFIC_PROCESS,COMBINATION,PRODUCT_SPECIFIC_RULE',
    rule_expression     VARCHAR(500)    NULL,
    rvc_threshold_pct   DECIMAL(8,4)    NULL,
    rvc_formula_code    VARCHAR(40)     NULL COMMENT 'FOB_VNM,BUILD_DOWN,BUILD_UP,CUSTOM',
    ctc_level           VARCHAR(10)     NULL COMMENT 'CC,CTH,CTSH',
    de_minimis_pct      DECIMAL(8,4)    NULL,
    cumulation_allowed  TINYINT(1)      NOT NULL DEFAULT 0,
    effective_from      DATE            NOT NULL,
    effective_to        DATE            NULL,
    source              VARCHAR(200)    NULL,
    version_no          INT             NOT NULL DEFAULT 1,
    approval_status     VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
    active              TINYINT(1)      NOT NULL DEFAULT 1,
    created_at          TIMESTAMP(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (origin_rule_id),
    KEY idx_or_agreement_hs (agreement_id, hs_code),
    KEY idx_or_effective (effective_from, effective_to),
    CONSTRAINT fk_or_agreement FOREIGN KEY (agreement_id) REFERENCES trade_agreement (agreement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS proof_of_origin (
    proof_id            CHAR(36)        NOT NULL,
    customer_id         CHAR(36)        NOT NULL,
    agreement_id        CHAR(36)        NULL,
    proof_type          VARCHAR(40)     NOT NULL COMMENT 'CERTIFICATE,DECLARATION,APPROVED_EXPORTER,DIGITAL',
    reference_number    VARCHAR(100)    NULL,
    issuer              VARCHAR(200)    NULL,
    producer            VARCHAR(200)    NULL,
    hs_lines_json       JSON            NULL,
    origin_criterion    VARCHAR(100)    NULL,
    document_ref        VARCHAR(200)    NULL,
    issue_date          DATE            NULL,
    expiry_date         DATE            NULL,
    verification_status VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
    created_at          TIMESTAMP(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          TIMESTAMP(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_by          VARCHAR(64)     NULL,
    PRIMARY KEY (proof_id),
    KEY idx_poo_customer (customer_id),
    KEY idx_poo_status (customer_id, verification_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS origin_assessment (
    assessment_id       CHAR(36)        NOT NULL,
    customer_id         CHAR(36)        NOT NULL,
    agreement_id        CHAR(36)        NULL,
    product_code        VARCHAR(64)     NULL,
    hs_code             VARCHAR(32)     NOT NULL,
    origin_country      CHAR(2)         NOT NULL,
    export_country      CHAR(2)         NOT NULL,
    destination_country CHAR(2)         NOT NULL,
    assessment_date     DATE            NOT NULL,
    result_code         VARCHAR(20)     NOT NULL COMMENT 'ELIGIBLE,NOT_ELIGIBLE,REVIEW',
    reason              VARCHAR(500)    NULL,
    rvc_pct             DECIMAL(18,8)   NULL,
    classification_result VARCHAR(20)  NULL,
    cumulation_result   VARCHAR(20)     NULL,
    proof_id            CHAR(36)        NULL,
    rule_version        INT             NULL,
    preferential_rate   DECIMAL(18,8)   NULL,
    normal_rate         DECIMAL(18,8)   NULL,
    duty_benefit        DECIMAL(18,6)   NULL,
    lifecycle_status    VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
    snapshot_json       JSON            NULL,
    created_at          TIMESTAMP(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          TIMESTAMP(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    approved_by         VARCHAR(64)     NULL,
    approved_at         TIMESTAMP(3)    NULL,
    locked_at           TIMESTAMP(3)    NULL,
    PRIMARY KEY (assessment_id),
    KEY idx_oa_customer (customer_id),
    KEY idx_oa_result (customer_id, result_code),
    KEY idx_oa_status (customer_id, lifecycle_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS trade_remedy_rule (
    remedy_id           CHAR(36)        NOT NULL,
    customer_id         CHAR(36)        NULL COMMENT 'NULL=platform catalog',
    hs_code             VARCHAR(32)     NOT NULL,
    origin_country      CHAR(2)         NOT NULL,
    measure_type        VARCHAR(40)     NOT NULL COMMENT 'ANTI_DUMPING,COUNTERVAILING,SAFEGUARD',
    rate                DECIMAL(18,8)   NULL,
    specific_amount     DECIMAL(18,6)   NULL,
    producer            VARCHAR(200)    NULL,
    exporter            VARCHAR(200)    NULL,
    effective_from      DATE            NOT NULL,
    effective_to        DATE            NULL,
    source              VARCHAR(200)    NULL,
    version_no          INT             NOT NULL DEFAULT 1,
    active              TINYINT(1)      NOT NULL DEFAULT 1,
    created_at          TIMESTAMP(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (remedy_id),
    KEY idx_trr_hs_origin (hs_code, origin_country),
    KEY idx_trr_effective (effective_from, effective_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS fta_snapshot (
    snapshot_id         CHAR(36)        NOT NULL,
    assessment_id       CHAR(36)        NOT NULL,
    customer_id         CHAR(36)        NOT NULL,
    snapshot_type       VARCHAR(40)     NOT NULL,
    payload_json        JSON            NOT NULL,
    created_at          TIMESTAMP(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (snapshot_id),
    KEY idx_fs_assessment (assessment_id),
    KEY idx_fs_customer (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
