INSERT INTO feature_catalog (feature_id, feature_code, feature_name, module_code, description, active)
VALUES
  ('feat-cm-std', 'COST_METHOD_STANDARD', 'Standard Cost Method', 'PRODUCT_COSTING', 'Standard', 1),
  ('feat-cm-act', 'COST_METHOD_ACTUAL', 'Actual Cost Method', 'PRODUCT_COSTING', 'Actual', 1),
  ('feat-cm-should', 'COST_METHOD_SHOULD', 'Should Cost Method', 'PRODUCT_COSTING', 'Should', 1),
  ('feat-cm-abc', 'COST_METHOD_ABC', 'ABC Cost Method', 'PRODUCT_COSTING', 'ABC', 1),
  ('feat-cm-job', 'COST_METHOD_JOB', 'Job Cost Method', 'PRODUCT_COSTING', 'Job', 1),
  ('feat-cm-process', 'COST_METHOD_PROCESS', 'Process Cost Method', 'PRODUCT_COSTING', 'Process', 1),
  ('feat-driver-cfg', 'COST_DRIVER_CONFIGURATION', 'Cost Driver Configuration', 'PRODUCT_COSTING', 'Drivers', 1),
  ('feat-pool-cfg', 'COST_POOL_CONFIGURATION', 'Cost Pool Configuration', 'PRODUCT_COSTING', 'Pools', 1),
  ('feat-ind-profile', 'INDUSTRY_PROFILE_CONFIGURATION', 'Industry Profile Configuration', 'PRODUCT_COSTING', 'Profiles', 1)
ON DUPLICATE KEY UPDATE feature_name = VALUES(feature_name);

CREATE TABLE IF NOT EXISTS cost_object (
    cost_object_id CHAR(36) NOT NULL PRIMARY KEY,
    customer_id CHAR(36) NOT NULL,
    object_type VARCHAR(40) NOT NULL,
    object_code VARCHAR(64) NOT NULL,
    object_name VARCHAR(200) NOT NULL,
    currency_code CHAR(3) NOT NULL DEFAULT 'USD',
    uom VARCHAR(20) NULL,
    reference_id VARCHAR(64) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uq_cost_object_cust_code (customer_id, object_code),
    KEY idx_cost_object_customer (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS cost_driver (
    cost_driver_id CHAR(36) NOT NULL PRIMARY KEY,
    customer_id CHAR(36) NOT NULL,
    driver_code VARCHAR(64) NOT NULL,
    driver_name VARCHAR(200) NOT NULL,
    driver_type VARCHAR(40) NOT NULL,
    uom VARCHAR(20) NULL,
    description VARCHAR(500) NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    version_no INT NOT NULL DEFAULT 1,
    lifecycle_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uq_cost_driver_cust_code (customer_id, driver_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS cost_pool (
    cost_pool_id CHAR(36) NOT NULL PRIMARY KEY,
    customer_id CHAR(36) NOT NULL,
    pool_code VARCHAR(64) NOT NULL,
    pool_name VARCHAR(200) NOT NULL,
    total_amount DECIMAL(18,6) NOT NULL DEFAULT 0,
    currency_code CHAR(3) NOT NULL DEFAULT 'USD',
    cost_driver_id CHAR(36) NULL,
    period_code VARCHAR(20) NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    version_no INT NOT NULL DEFAULT 1,
    lifecycle_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS universal_cost_calculation (
    calculation_id CHAR(36) NOT NULL PRIMARY KEY,
    customer_id CHAR(36) NOT NULL,
    cost_object_id CHAR(36) NOT NULL,
    cost_method VARCHAR(40) NOT NULL,
    calculation_date DATE NOT NULL,
    as_of_date DATE NULL,
    currency_code CHAR(3) NOT NULL DEFAULT 'USD',
    total_cost DECIMAL(18,6) NOT NULL DEFAULT 0,
    unit_cost DECIMAL(18,6) NULL,
    quantity DECIMAL(18,6) NULL,
    component_breakdown JSON NULL,
    driver_usage_json JSON NULL,
    allocation_json JSON NULL,
    version_no INT NOT NULL DEFAULT 1,
    lifecycle_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    industry_profile VARCHAR(64) NULL,
    created_by VARCHAR(64) NULL,
    approved_by VARCHAR(64) NULL,
    approved_at TIMESTAMP(3) NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    KEY idx_ucc_customer (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS industry_costing_profile (
    profile_id CHAR(36) NOT NULL PRIMARY KEY,
    profile_code VARCHAR(64) NOT NULL,
    profile_name VARCHAR(200) NOT NULL,
    description VARCHAR(500) NULL,
    default_methods_json JSON NULL,
    default_components_json JSON NULL,
    default_drivers_json JSON NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    UNIQUE KEY uq_industry_profile_code (profile_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO industry_costing_profile (profile_id, profile_code, profile_name, description, default_methods_json, default_components_json, default_drivers_json, active)
VALUES
('ip-discrete', 'DISCRETE_MANUFACTURING', 'Discrete Manufacturing', 'BOM/routing oriented', '["STANDARD","SHOULD","ACTUAL"]', '["MATERIAL","LABOR","MACHINE","OVERHEAD"]', '["QUANTITY","MACHINE_HOURS"]', 1),
('ip-process', 'PROCESS_MANUFACTURING', 'Process Manufacturing', 'Batch/process', '["PROCESS","ACTUAL","STANDARD"]', '["MATERIAL","ENERGY","LABOR","OVERHEAD"]', '["VOLUME","BATCH_COUNT"]', 1),
('ip-services', 'PROFESSIONAL_SERVICES', 'Professional Services', 'Hour based', '["PROJECT","ACTUAL","QUOTATION"]', '["LABOR","OVERHEAD"]', '["SERVICE_HOURS"]', 1)
ON DUPLICATE KEY UPDATE profile_name = VALUES(profile_name);
