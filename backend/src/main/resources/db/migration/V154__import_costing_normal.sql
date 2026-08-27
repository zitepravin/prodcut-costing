-- V154: Normal Import Costing / Landed Cost (industry-neutral)
-- FTA / preferential tariff is OUT OF SCOPE

INSERT INTO feature_catalog (feature_id, feature_code, feature_name, module_code, description, active)
VALUES
  ('feat-import-costing', 'IMPORT_COSTING', 'Import Costing / Landed Cost', 'PRODUCT_COSTING',
   'Normal import and landed cost calculation (non-preferential)', 1)
ON DUPLICATE KEY UPDATE feature_name = VALUES(feature_name);

CREATE TABLE IF NOT EXISTS import_duty_rule (
    duty_rule_id        CHAR(36)        NOT NULL,
    customer_id         CHAR(36)        NOT NULL,
    hs_code             VARCHAR(32)     NOT NULL,
    origin_country      CHAR(2)         NOT NULL,
    destination_country CHAR(2)         NOT NULL,
    duty_type           VARCHAR(20)     NOT NULL COMMENT 'AD_VALOREM,SPECIFIC,MIXED,TIERED,EXEMPT',
    ad_valorem_rate     DECIMAL(18,8)   NULL,
    specific_amount     DECIMAL(18,6)   NULL,
    specific_currency   CHAR(3)         NULL,
    taxable_base        VARCHAR(40)     NOT NULL DEFAULT 'CUSTOMS_VALUE',
    effective_from      DATE            NOT NULL,
    effective_to        DATE            NULL,
    source              VARCHAR(100)    NULL,
    version_no          INT             NOT NULL DEFAULT 1,
    approval_status     VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
    active              TINYINT(1)      NOT NULL DEFAULT 1,
    created_at          TIMESTAMP(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          TIMESTAMP(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_by          VARCHAR(64)     NULL,
    updated_by          VARCHAR(64)     NULL,
    PRIMARY KEY (duty_rule_id),
    KEY idx_idr_customer (customer_id),
    KEY idx_idr_hs_origin_dest (customer_id, hs_code, origin_country, destination_country),
    KEY idx_idr_effective (customer_id, effective_from, effective_to),
    KEY idx_idr_status (customer_id, approval_status, active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS import_tax_rule (
    tax_rule_id         CHAR(36)        NOT NULL,
    customer_id         CHAR(36)        NOT NULL,
    tax_code            VARCHAR(40)     NOT NULL,
    tax_type            VARCHAR(40)     NOT NULL,
    rate                DECIMAL(18,8)   NOT NULL,
    tax_base            VARCHAR(40)     NOT NULL DEFAULT 'CUSTOMS_VALUE_PLUS_DUTY',
    jurisdiction        VARCHAR(40)     NULL,
    destination_country CHAR(2)         NULL,
    recoverable         TINYINT(1)      NOT NULL DEFAULT 0,
    included_in_landed  TINYINT(1)      NOT NULL DEFAULT 1,
    effective_from      DATE            NOT NULL,
    effective_to        DATE            NULL,
    source              VARCHAR(100)    NULL,
    version_no          INT             NOT NULL DEFAULT 1,
    approval_status     VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
    active              TINYINT(1)      NOT NULL DEFAULT 1,
    created_at          TIMESTAMP(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          TIMESTAMP(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (tax_rule_id),
    KEY idx_itr_customer (customer_id),
    KEY idx_itr_code (customer_id, tax_code),
    KEY idx_itr_effective (customer_id, effective_from, effective_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS import_incoterm_rule (
    incoterm_rule_id    CHAR(36)        NOT NULL,
    customer_id         CHAR(36)        NOT NULL,
    incoterm_code       VARCHAR(10)     NOT NULL,
    seller_freight      TINYINT(1)      NOT NULL DEFAULT 0,
    seller_insurance    TINYINT(1)      NOT NULL DEFAULT 0,
    seller_duty         TINYINT(1)      NOT NULL DEFAULT 0,
    seller_local        TINYINT(1)      NOT NULL DEFAULT 0,
    description         VARCHAR(200)    NULL,
    active              TINYINT(1)      NOT NULL DEFAULT 1,
    version_no          INT             NOT NULL DEFAULT 1,
    created_at          TIMESTAMP(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          TIMESTAMP(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (incoterm_rule_id),
    UNIQUE KEY uq_iir_customer_code (customer_id, incoterm_code),
    KEY idx_iir_customer (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS import_allocation_rule (
    allocation_rule_id  CHAR(36)        NOT NULL,
    customer_id         CHAR(36)        NOT NULL,
    rule_code           VARCHAR(40)     NOT NULL,
    allocation_method   VARCHAR(40)     NOT NULL COMMENT 'VALUE,QUANTITY,NET_WEIGHT,GROSS_WEIGHT,VOLUME,CHARGEABLE_WEIGHT,CONTAINER,PALLET,CUSTOM_DRIVER',
    component_code      VARCHAR(40)     NULL COMMENT 'applies to component or ALL',
    active              TINYINT(1)      NOT NULL DEFAULT 1,
    version_no          INT             NOT NULL DEFAULT 1,
    created_at          TIMESTAMP(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          TIMESTAMP(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (allocation_rule_id),
    UNIQUE KEY uq_iar_customer_code (customer_id, rule_code),
    KEY idx_iar_customer (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS import_shipment (
    shipment_id         CHAR(36)        NOT NULL,
    customer_id         CHAR(36)        NOT NULL,
    shipment_ref        VARCHAR(64)     NOT NULL,
    supplier_id         VARCHAR(64)     NULL,
    supplier_country    CHAR(2)         NULL,
    origin_country      CHAR(2)         NOT NULL,
    export_country      CHAR(2)         NOT NULL,
    destination_country CHAR(2)         NOT NULL,
    shipment_mode       VARCHAR(20)     NULL COMMENT 'AIR,SEA,ROAD,RAIL,COURIER',
    incoterm_code       VARCHAR(10)     NULL,
    shipment_date       DATE            NULL,
    customs_date        DATE            NULL,
    currency_code       CHAR(3)         NOT NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
    created_at          TIMESTAMP(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          TIMESTAMP(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_by          VARCHAR(64)     NULL,
    PRIMARY KEY (shipment_id),
    UNIQUE KEY uq_is_customer_ref (customer_id, shipment_ref),
    KEY idx_is_customer (customer_id),
    KEY idx_is_status (customer_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS import_shipment_item (
    item_id             CHAR(36)        NOT NULL,
    shipment_id         CHAR(36)        NOT NULL,
    customer_id         CHAR(36)        NOT NULL,
    line_no             INT             NOT NULL,
    material_id         VARCHAR(64)     NULL,
    product_code        VARCHAR(64)     NULL,
    hs_code             VARCHAR(32)     NOT NULL,
    quantity            DECIMAL(18,6)   NOT NULL,
    uom                 VARCHAR(20)     NULL,
    net_weight          DECIMAL(18,6)   NULL,
    gross_weight        DECIMAL(18,6)   NULL,
    volume              DECIMAL(18,6)   NULL,
    unit_price          DECIMAL(18,6)   NOT NULL,
    invoice_currency    CHAR(3)         NOT NULL,
    invoice_value       DECIMAL(18,6)   NOT NULL,
    created_at          TIMESTAMP(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (item_id),
    KEY idx_isi_shipment (shipment_id),
    KEY idx_isi_customer (customer_id),
    CONSTRAINT fk_isi_shipment FOREIGN KEY (shipment_id) REFERENCES import_shipment (shipment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS import_cost_calculation (
    calculation_id      CHAR(36)        NOT NULL,
    customer_id         CHAR(36)        NOT NULL,
    shipment_id         CHAR(36)        NULL,
    item_id             CHAR(36)        NULL,
    costing_date        DATE            NOT NULL,
    fx_as_of_date       DATE            NOT NULL,
    currency_code       CHAR(3)         NOT NULL,
    invoice_value       DECIMAL(18,6)   NULL,
    customs_value       DECIMAL(18,6)   NULL,
    total_landed_cost   DECIMAL(18,6)   NULL,
    unit_landed_cost    DECIMAL(18,6)   NULL,
    quantity            DECIMAL(18,6)   NULL,
    lifecycle_status    VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
    review_status       VARCHAR(40)     NULL COMMENT 'OK,MISSING_DATA,REVIEW',
    missing_data_json   JSON            NULL,
    snapshot_json       JSON            NULL,
    industry_profile    VARCHAR(64)     NULL,
    created_at          TIMESTAMP(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          TIMESTAMP(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_by          VARCHAR(64)     NULL,
    approved_by         VARCHAR(64)     NULL,
    approved_at         TIMESTAMP(3)    NULL,
    locked_at           TIMESTAMP(3)    NULL,
    version_no          INT             NOT NULL DEFAULT 1,
    PRIMARY KEY (calculation_id),
    KEY idx_icc_customer (customer_id),
    KEY idx_icc_shipment (shipment_id),
    KEY idx_icc_status (customer_id, lifecycle_status),
    KEY idx_icc_costing_date (customer_id, costing_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS import_cost_component (
    component_id        CHAR(36)        NOT NULL,
    calculation_id      CHAR(36)        NOT NULL,
    customer_id         CHAR(36)        NOT NULL,
    component_code      VARCHAR(40)     NOT NULL,
    amount              DECIMAL(18,6)   NOT NULL,
    currency_code       CHAR(3)         NOT NULL,
    recoverable         TINYINT(1)      NOT NULL DEFAULT 0,
    included_in_landed  TINYINT(1)      NOT NULL DEFAULT 1,
    allocation_method   VARCHAR(40)     NULL,
    allocation_pct      DECIMAL(18,8)   NULL,
    driver_quantity     DECIMAL(18,6)   NULL,
    source_ref          VARCHAR(100)    NULL,
    formula_note        VARCHAR(500)    NULL,
    sort_order          INT             NOT NULL DEFAULT 0,
    PRIMARY KEY (component_id),
    KEY idx_icomp_calc (calculation_id),
    KEY idx_icomp_customer (customer_id),
    CONSTRAINT fk_icomp_calc FOREIGN KEY (calculation_id) REFERENCES import_cost_calculation (calculation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS import_cost_snapshot (
    snapshot_id         CHAR(36)        NOT NULL,
    calculation_id      CHAR(36)        NOT NULL,
    customer_id         CHAR(36)        NOT NULL,
    snapshot_type       VARCHAR(40)     NOT NULL COMMENT 'FX,DUTY,TAX,INCOTERM,FREIGHT,ALLOCATION,CONFIG,INPUTS',
    payload_json        JSON            NOT NULL,
    created_at          TIMESTAMP(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (snapshot_id),
    KEY idx_ics_calc (calculation_id),
    KEY idx_ics_customer (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Default Incoterm responsibility templates (global seed rows use system customer placeholder '__SYSTEM__')
INSERT INTO import_incoterm_rule (incoterm_rule_id, customer_id, incoterm_code, seller_freight, seller_insurance, seller_duty, seller_local, description, active, version_no)
VALUES
  ('icr-exw', '__SYSTEM__', 'EXW', 0, 0, 0, 0, 'Ex Works — buyer bears almost all costs', 1, 1),
  ('icr-fca', '__SYSTEM__', 'FCA', 0, 0, 0, 0, 'Free Carrier', 1, 1),
  ('icr-fob', '__SYSTEM__', 'FOB', 0, 0, 0, 0, 'Free On Board — buyer pays main freight', 1, 1),
  ('icr-cfr', '__SYSTEM__', 'CFR', 1, 0, 0, 0, 'Cost and Freight — seller pays freight', 1, 1),
  ('icr-cif', '__SYSTEM__', 'CIF', 1, 1, 0, 0, 'Cost Insurance Freight — seller pays freight+insurance', 1, 1),
  ('icr-dap', '__SYSTEM__', 'DAP', 1, 1, 0, 1, 'Delivered at Place', 1, 1),
  ('icr-ddp', '__SYSTEM__', 'DDP', 1, 1, 1, 1, 'Delivered Duty Paid — seller pays duty', 1, 1)
ON DUPLICATE KEY UPDATE description = VALUES(description);
