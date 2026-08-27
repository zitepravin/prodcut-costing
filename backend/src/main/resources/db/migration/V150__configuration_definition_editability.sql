CREATE TABLE IF NOT EXISTS configuration_definition (
    definition_id CHAR(36) NOT NULL PRIMARY KEY,
    config_area VARCHAR(64) NOT NULL,
    config_key VARCHAR(128) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    data_type VARCHAR(20) NOT NULL DEFAULT 'STRING',
    default_value TEXT NULL,
    editable TINYINT(1) NOT NULL DEFAULT 1,
    required_flag TINYINT(1) NOT NULL DEFAULT 0,
    scope_level VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
    description VARCHAR(500) NULL,
    classification VARCHAR(40) NOT NULL DEFAULT 'BUSINESS',
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uq_config_def_area_key (config_area, config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO configuration_definition
(definition_id, config_area, config_key, display_name, data_type, default_value, editable, required_flag, scope_level, description, classification, active)
VALUES
('cdef-fx-scale', 'FX', 'fx.money.scale', 'FX Money Scale', 'INTEGER', '6', 0, 1, 'SYSTEM', 'Money scale', 'FINANCIAL_SAFETY', 1),
('cdef-tenant', 'APPROVAL', 'security.tenant.isolation.enforced', 'Tenant Isolation', 'BOOLEAN', 'true', 0, 1, 'SYSTEM', 'Cannot disable', 'SECURITY_CONTROL', 1),
('cdef-fx-src', 'FX', 'fx.preferred.source', 'Preferred FX Source', 'STRING', 'MANUAL', 1, 0, 'CUSTOMER', 'FX source', 'BUSINESS', 1)
ON DUPLICATE KEY UPDATE editable = VALUES(editable);
