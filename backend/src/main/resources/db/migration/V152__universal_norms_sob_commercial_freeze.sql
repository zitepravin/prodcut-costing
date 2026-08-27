-- V152 Norms, SOB, commercial, freeze
CREATE TABLE IF NOT EXISTS norm_definition (
    norm_id CHAR(36) NOT NULL PRIMARY KEY,
    customer_id CHAR(36) NOT NULL,
    norm_code VARCHAR(64) NOT NULL,
    description VARCHAR(500) NULL,
    unit VARCHAR(20) NULL,
    parameter_key VARCHAR(64) NULL,
    formula_code VARCHAR(64) NULL,
    norm_value DECIMAL(18,8) NOT NULL,
    object_scope VARCHAR(40) NULL,
    object_reference_id VARCHAR(64) NULL,
    effective_from DATE NOT NULL,
    effective_to DATE NULL,
    version_no INT NOT NULL DEFAULT 1,
    active TINYINT(1) NOT NULL DEFAULT 1,
    approval_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    source VARCHAR(100) NULL,
    created_by VARCHAR(64) NULL,
    approved_by VARCHAR(64) NULL,
    approved_at TIMESTAMP(3) NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uq_norm_cust_code_ver (customer_id, norm_code, version_no),
    KEY idx_norm_customer (customer_id),
    KEY idx_norm_effective (customer_id, norm_code, effective_from, effective_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS supplier_share_of_business (
    sob_id CHAR(36) NOT NULL PRIMARY KEY,
    customer_id CHAR(36) NOT NULL,
    cost_object_id CHAR(36) NULL,
    product_code VARCHAR(64) NULL,
    supplier_id CHAR(36) NOT NULL,
    allocation_percent DECIMAL(9,4) NOT NULL,
    allocation_basis VARCHAR(40) NOT NULL DEFAULT 'VOLUME',
    allocation_group_code VARCHAR(64) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    priority INT NOT NULL DEFAULT 1,
    version_no INT NOT NULL DEFAULT 1,
    created_by VARCHAR(64) NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    KEY idx_sob_customer (customer_id),
    KEY idx_sob_group (customer_id, allocation_group_code, effective_from)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE universal_cost_calculation
    ADD COLUMN technical_total DECIMAL(18,6) NULL,
    ADD COLUMN commercial_total DECIMAL(18,6) NULL,
    ADD COLUMN selling_price DECIMAL(18,6) NULL,
    ADD COLUMN discount_amount DECIMAL(18,6) NULL,
    ADD COLUMN royalty_amount DECIMAL(18,6) NULL,
    ADD COLUMN profit_margin_amount DECIMAL(18,6) NULL,
    ADD COLUMN commercial_expense_amount DECIMAL(18,6) NULL,
    ADD COLUMN negotiation_adjustment DECIMAL(18,6) NULL,
    ADD COLUMN norm_snapshot_json JSON NULL,
    ADD COLUMN sob_snapshot_json JSON NULL,
    ADD COLUMN config_snapshot_json JSON NULL,
    ADD COLUMN locked_at TIMESTAMP(3) NULL,
    ADD COLUMN locked_by VARCHAR(64) NULL,
    ADD COLUMN submitted_at TIMESTAMP(3) NULL,
    ADD COLUMN submitted_by VARCHAR(64) NULL;

INSERT INTO feature_catalog (feature_id, feature_code, feature_name, module_code, description, active)
VALUES
  ('feat-norm-cfg', 'NORM_DEFINITION_CONFIGURATION', 'Norm Definition Configuration', 'PRODUCT_COSTING', 'Norms', 1),
  ('feat-sob-cfg', 'SUPPLIER_SOB_CONFIGURATION', 'Supplier Share of Business', 'PRODUCT_COSTING', 'SOB', 1),
  ('feat-commercial', 'COMMERCIAL_COST_LAYER', 'Commercial Cost Layer', 'PRODUCT_COSTING', 'Commercial', 1)
ON DUPLICATE KEY UPDATE feature_name = VALUES(feature_name);
