CREATE TABLE IF NOT EXISTS feature_catalog (
    feature_id      CHAR(36)     NOT NULL,
    feature_code    VARCHAR(64)  NOT NULL,
    feature_name    VARCHAR(200) NOT NULL,
    module_code     VARCHAR(64)  NOT NULL,
    description     VARCHAR(500) NULL,
    active          TINYINT(1)   NOT NULL DEFAULT 1,
    created_at      TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (feature_id),
    UNIQUE KEY uq_feature_catalog_code (feature_code),
    KEY idx_feature_catalog_module (module_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS customer_feature_entitlement (
    entitlement_id  CHAR(36)     NOT NULL,
    customer_id     CHAR(36)     NOT NULL,
    feature_id      CHAR(36)     NOT NULL,
    enabled         TINYINT(1)   NOT NULL DEFAULT 1,
    effective_from  DATE         NULL,
    effective_to    DATE         NULL,
    created_at      TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_by      VARCHAR(64)  NULL,
    updated_by      VARCHAR(64)  NULL,
    PRIMARY KEY (entitlement_id),
    UNIQUE KEY uq_cfe_customer_feature (customer_id, feature_id),
    KEY idx_cfe_customer (customer_id),
    KEY idx_cfe_feature (feature_id),
    CONSTRAINT fk_cfe_feature FOREIGN KEY (feature_id) REFERENCES feature_catalog (feature_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO feature_catalog (feature_id, feature_code, feature_name, module_code, description, active)
VALUES
  ('feat-should-cost-001', 'SHOULD_COST_CALCULATE', 'Should-Cost Calculate', 'PRODUCT_COSTING', 'Calculate and persist should-cost build-ups', 1),
  ('feat-should-cost-002', 'SHOULD_COST_APPROVE', 'Should-Cost Approve', 'PRODUCT_COSTING', 'Approve and lock should-cost', 1),
  ('feat-fx-historical', 'FX_HISTORICAL', 'Historical FX Conversion', 'PRODUCT_COSTING', 'Convert using explicit as-of / historical rates', 1),
  ('feat-costing-sheet', 'COSTING_SHEET', 'Costing Sheet Overhead', 'PRODUCT_COSTING', 'Configurable costing sheet and overhead rules', 1),
  ('feat-actual-cost', 'ACTUAL_COST', 'Actual Cost Import', 'PRODUCT_COSTING', 'Import and compare actual cost records', 1)
ON DUPLICATE KEY UPDATE feature_name = VALUES(feature_name);
