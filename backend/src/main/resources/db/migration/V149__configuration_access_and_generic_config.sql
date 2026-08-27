INSERT INTO feature_catalog (feature_id, feature_code, feature_name, module_code, description, active)
VALUES
  ('feat-cfg-fx', 'FX_CONFIGURATION', 'FX Configuration', 'PRODUCT_COSTING', 'FX configuration', 1),
  ('feat-cfg-overhead', 'OVERHEAD_CONFIGURATION', 'Overhead Configuration', 'PRODUCT_COSTING', 'Overhead configuration', 1),
  ('feat-cfg-sheet', 'COSTING_SHEET_CONFIGURATION', 'Costing Sheet Configuration', 'PRODUCT_COSTING', 'Costing sheet configuration', 1),
  ('feat-cfg-actual', 'ACTUAL_COST_CONFIGURATION', 'Actual Cost Configuration', 'PRODUCT_COSTING', 'Actual cost configuration', 1),
  ('feat-cfg-approval', 'APPROVAL_CONFIGURATION', 'Approval Configuration', 'PRODUCT_COSTING', 'Approval configuration', 1),
  ('feat-cfg-material', 'MATERIAL_CONFIGURATION', 'Material Configuration', 'PRODUCT_COSTING', 'Material configuration', 1),
  ('feat-cfg-machine', 'MACHINE_CONFIGURATION', 'Machine Configuration', 'PRODUCT_COSTING', 'Machine configuration', 1),
  ('feat-cfg-labor', 'LABOR_CONFIGURATION', 'Labor Configuration', 'PRODUCT_COSTING', 'Labor configuration', 1)
ON DUPLICATE KEY UPDATE feature_name = VALUES(feature_name);

CREATE TABLE IF NOT EXISTS customer_configuration (
    config_id CHAR(36) NOT NULL PRIMARY KEY,
    customer_id CHAR(36) NOT NULL,
    config_area VARCHAR(64) NOT NULL,
    config_key VARCHAR(128) NOT NULL,
    value_text TEXT NULL,
    data_type VARCHAR(20) NOT NULL DEFAULT 'STRING',
    description VARCHAR(500) NULL,
    default_value TEXT NULL,
    program_id CHAR(36) NULL,
    scope_level VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
    overridable TINYINT(1) NOT NULL DEFAULT 1,
    effective_from DATE NULL,
    effective_to DATE NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    version_no INT NOT NULL DEFAULT 1,
    lifecycle_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_by VARCHAR(64) NULL,
    updated_by VARCHAR(64) NULL,
    approved_by VARCHAR(64) NULL,
    approved_at TIMESTAMP(3) NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    KEY idx_cc_customer (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS customer_configuration_history (
    history_id CHAR(36) NOT NULL PRIMARY KEY,
    config_id CHAR(36) NOT NULL,
    customer_id CHAR(36) NOT NULL,
    config_area VARCHAR(64) NOT NULL,
    config_key VARCHAR(128) NOT NULL,
    old_value TEXT NULL,
    new_value TEXT NULL,
    version_no INT NOT NULL,
    action_code VARCHAR(40) NOT NULL,
    reason VARCHAR(500) NULL,
    actor_user_id VARCHAR(64) NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    KEY idx_cch_customer (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
