-- V161 Phase 2: Platform permission availability + customer role permission grants
-- Extends existing module/feature entitlement (V32/V147). Does not replace role JWT auth.

-- Catalog metadata: maps existing permission codes to module/feature/function labels
CREATE TABLE IF NOT EXISTS permission_catalog_entry (
    permission_code   VARCHAR(64)  NOT NULL,
    module_code       VARCHAR(64)  NOT NULL,
    feature_code      VARCHAR(64)  NOT NULL,
    function_code     VARCHAR(64)  NOT NULL,
    function_name     VARCHAR(200) NOT NULL,
    description       VARCHAR(500) NULL,
    active            TINYINT(1)   NOT NULL DEFAULT 1,
    sort_order        INT          NOT NULL DEFAULT 0,
    created_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at        DATETIME(6)  NULL,
    PRIMARY KEY (permission_code),
    KEY idx_pce_module_feature (module_code, feature_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Platform layer: is this permission available to the customer at all?
CREATE TABLE IF NOT EXISTS customer_permission_entitlement (
    entitlement_id    CHAR(36)     NOT NULL,
    customer_id       CHAR(36)     NOT NULL,
    permission_code   VARCHAR(64)  NOT NULL,
    allowed           TINYINT(1)   NOT NULL DEFAULT 1,
    created_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at        DATETIME(6)  NULL,
    created_by        VARCHAR(64)  NULL,
    updated_by        VARCHAR(64)  NULL,
    PRIMARY KEY (entitlement_id),
    UNIQUE KEY uq_cpe_customer_perm (customer_id, permission_code),
    KEY idx_cpe_customer (customer_id),
    CONSTRAINT fk_cpe_customer FOREIGN KEY (customer_id) REFERENCES platform_customer (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Customer Admin layer: role grant within customer (cannot exceed platform entitlement)
CREATE TABLE IF NOT EXISTS customer_role_permission (
    grant_id          CHAR(36)     NOT NULL,
    customer_id       CHAR(36)     NOT NULL,
    role_code         VARCHAR(50)  NOT NULL,
    permission_code   VARCHAR(64)  NOT NULL,
    allowed           TINYINT(1)   NOT NULL DEFAULT 1,
    created_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at        DATETIME(6)  NULL,
    created_by        VARCHAR(64)  NULL,
    updated_by        VARCHAR(64)  NULL,
    PRIMARY KEY (grant_id),
    UNIQUE KEY uq_crp_customer_role_perm (customer_id, role_code, permission_code),
    KEY idx_crp_customer_role (customer_id, role_code),
    CONSTRAINT fk_crp_customer FOREIGN KEY (customer_id) REFERENCES platform_customer (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed catalog entries from existing permission codes (module/feature/function labels)
INSERT INTO permission_catalog_entry (permission_code, module_code, feature_code, function_code, function_name, description, sort_order) VALUES
('BOM_VIEW', 'PRODUCT_COSTING', 'BOM', 'VIEW', 'View BOM', 'View BOM structure', 10),
('BOM_CREATE', 'PRODUCT_COSTING', 'BOM', 'CREATE', 'Create BOM', 'Create BOM', 20),
('BOM_EDIT', 'PRODUCT_COSTING', 'BOM', 'EDIT', 'Edit BOM', 'Edit BOM', 30),
('BOM_DELETE', 'PRODUCT_COSTING', 'BOM', 'DELETE', 'Delete BOM', 'Delete BOM', 40),
('COSTING_VIEW', 'PRODUCT_COSTING', 'COSTING', 'VIEW', 'View Costing', 'View cost estimates', 50),
('COSTING_CREATE', 'PRODUCT_COSTING', 'COSTING', 'CREATE', 'Create Costing', 'Create costing', 60),
('COSTING_EDIT', 'PRODUCT_COSTING', 'COSTING', 'EDIT', 'Edit Costing', 'Edit costing', 70),
('COSTING_APPROVE', 'PRODUCT_COSTING', 'COSTING', 'APPROVE', 'Approve Costing', 'Approve costing', 80),
('SHOULD_COST', 'PRODUCT_COSTING', 'SHOULD_COST', 'EXECUTE', 'Should-Cost', 'Should-cost analysis', 90),
('MAKE_VS_BUY', 'PRODUCT_COSTING', 'MAKE_VS_BUY', 'EXECUTE', 'Make vs Buy', 'Make vs Buy', 100),
('RFQ_VIEW', 'VENDOR_SELECTION', 'RFQ', 'VIEW', 'View RFQ', 'View RFQs', 110),
('RFQ_CREATE', 'VENDOR_SELECTION', 'RFQ', 'CREATE', 'Create RFQ', 'Create RFQ', 120),
('RFQ_EDIT', 'VENDOR_SELECTION', 'RFQ', 'EDIT', 'Edit RFQ', 'Edit RFQ', 130),
('RFQ_DELETE', 'VENDOR_SELECTION', 'RFQ', 'DELETE', 'Delete RFQ', 'Delete RFQ', 140),
('SUPPLIER_VIEW', 'VENDOR_SELECTION', 'SUPPLIER', 'VIEW', 'View Supplier', 'View suppliers', 150),
('SUPPLIER_CREATE', 'VENDOR_SELECTION', 'SUPPLIER', 'CREATE', 'Create Supplier', 'Create supplier', 160),
('SUPPLIER_EDIT', 'VENDOR_SELECTION', 'SUPPLIER', 'EDIT', 'Edit Supplier', 'Edit supplier', 170),
('QUOTE_VIEW', 'VENDOR_SELECTION', 'QUOTE', 'VIEW', 'View Quote', 'View quotes', 180),
('QUOTE_CREATE', 'VENDOR_SELECTION', 'QUOTE', 'CREATE', 'Create Quote', 'Create quote', 190),
('QUOTE_EDIT', 'VENDOR_SELECTION', 'QUOTE', 'EDIT', 'Edit Quote', 'Edit quote', 200),
('REPORT_VIEW', 'PRODUCT_COSTING', 'REPORTS', 'VIEW', 'View Reports', 'View reports', 210),
('DASHBOARD_VIEW', 'PRODUCT_COSTING', 'DASHBOARD', 'VIEW', 'View Dashboard', 'View dashboards', 220),
('USER_VIEW', 'ADMINISTRATION', 'USERS', 'VIEW', 'View Users', 'View users in own tenant', 230),
('USER_CREATE', 'ADMINISTRATION', 'USERS', 'CREATE', 'Create Users', 'Create users', 240),
('USER_EDIT', 'ADMINISTRATION', 'USERS', 'EDIT', 'Edit Users', 'Edit users', 250),
('USER_DISABLE', 'ADMINISTRATION', 'USERS', 'DISABLE', 'Disable Users', 'Activate/deactivate users', 260),
('ROLE_VIEW', 'ADMINISTRATION', 'ROLES', 'VIEW', 'View Roles', 'View roles', 270),
('ROLE_MANAGE', 'ADMINISTRATION', 'ROLES', 'MANAGE', 'Manage Roles', 'Manage custom roles', 280),
('AUDIT_VIEW', 'ADMINISTRATION', 'AUDIT', 'VIEW', 'View Audit', 'View audit log', 290),
('CUSTOMER_BRANDING_READ', 'ADMINISTRATION', 'BRANDING', 'READ', 'View Branding', 'View company logo', 300),
('CUSTOMER_BRANDING_MANAGE', 'ADMINISTRATION', 'BRANDING', 'MANAGE', 'Manage Branding', 'Upload/replace company logo', 310)
ON DUPLICATE KEY UPDATE function_name = VALUES(function_name);
