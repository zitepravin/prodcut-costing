-- V32: Central Access Management (Stage A1) — customer/tenant, module registry, entitlements
-- Does not replace app_user RBAC; layers tenant + module enablement for multi-product platform.

CREATE TABLE platform_customer (
    customer_id     VARCHAR(36)  NOT NULL,
    customer_code   VARCHAR(50)  NOT NULL,
    customer_name   VARCHAR(200) NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_by      VARCHAR(36),
    created_at      DATETIME(6)  NOT NULL,
    updated_by      VARCHAR(36),
    updated_at      DATETIME(6),
    PRIMARY KEY (customer_id),
    UNIQUE KEY uk_platform_customer_code (customer_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE platform_module (
    module_id       VARCHAR(36)  NOT NULL,
    module_code     VARCHAR(50)  NOT NULL,
    module_name     VARCHAR(150) NOT NULL,
    description     VARCHAR(500),
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_by      VARCHAR(36),
    created_at      DATETIME(6)  NOT NULL,
    updated_by      VARCHAR(36),
    updated_at      DATETIME(6),
    PRIMARY KEY (module_id),
    UNIQUE KEY uk_platform_module_code (module_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE customer_module_entitlement (
    entitlement_id  VARCHAR(36)  NOT NULL,
    customer_id     VARCHAR(36)  NOT NULL,
    module_id       VARCHAR(36)  NOT NULL,
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    effective_from  DATE,
    effective_to    DATE,
    created_by      VARCHAR(36),
    created_at      DATETIME(6)  NOT NULL,
    updated_by      VARCHAR(36),
    updated_at      DATETIME(6),
    PRIMARY KEY (entitlement_id),
    UNIQUE KEY uk_customer_module (customer_id, module_id),
    CONSTRAINT fk_cme_customer FOREIGN KEY (customer_id) REFERENCES platform_customer (customer_id),
    CONSTRAINT fk_cme_module FOREIGN KEY (module_id) REFERENCES platform_module (module_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE customer_user_membership (
    membership_id   VARCHAR(36)  NOT NULL,
    customer_id     VARCHAR(36)  NOT NULL,
    user_id         VARCHAR(36)  NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_by      VARCHAR(36),
    created_at      DATETIME(6)  NOT NULL,
    updated_by      VARCHAR(36),
    updated_at      DATETIME(6),
    PRIMARY KEY (membership_id),
    UNIQUE KEY uk_customer_user (customer_id, user_id),
    INDEX idx_cum_user (user_id, status),
    CONSTRAINT fk_cum_customer FOREIGN KEY (customer_id) REFERENCES platform_customer (customer_id),
    CONSTRAINT fk_cum_user FOREIGN KEY (user_id) REFERENCES app_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed platform modules (Product Costing + Vendor Selection + future placeholders)
INSERT INTO platform_module (module_id, module_code, module_name, description, status, created_at) VALUES
    (UUID(), 'PRODUCT_COSTING', 'Product Costing', 'BOM, should-cost, RFQ, target costing, reporting', 'ACTIVE', CURRENT_TIMESTAMP(6)),
    (UUID(), 'VENDOR_SELECTION', 'Vendor Selection', 'Supplier evaluation, TCO, scorecard, recommendation', 'ACTIVE', CURRENT_TIMESTAMP(6)),
    (UUID(), 'PURCHASE', 'Purchase', 'Future purchase module', 'ACTIVE', CURRENT_TIMESTAMP(6)),
    (UUID(), 'ORDER_TRACKING', 'Order Tracking', 'Future order tracking module', 'ACTIVE', CURRENT_TIMESTAMP(6));

-- Default single-tenant customer for existing deployments (all current users can be linked later)
INSERT INTO platform_customer (customer_id, customer_code, customer_name, status, created_at) VALUES
    (UUID(), 'DEFAULT', 'Default Customer', 'ACTIVE', CURRENT_TIMESTAMP(6));

INSERT INTO customer_module_entitlement (entitlement_id, customer_id, module_id, enabled, created_at)
SELECT UUID(), c.customer_id, m.module_id, TRUE, CURRENT_TIMESTAMP(6)
FROM platform_customer c
CROSS JOIN platform_module m
WHERE c.customer_code = 'DEFAULT'
  AND m.module_code IN ('PRODUCT_COSTING', 'VENDOR_SELECTION');
