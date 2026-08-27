-- V91: Payment & Subscription Management (commercial entitlement layer)
-- Payment never bypasses Central Access; only verified backend events activate entitlements.

CREATE TABLE billing_plan (
    plan_id             CHAR(36)       NOT NULL,
    plan_code           VARCHAR(50)    NOT NULL,
    plan_name           VARCHAR(150)   NOT NULL,
    description         VARCHAR(500),
    billing_frequency   VARCHAR(20)    NOT NULL,
    price               DECIMAL(19,4)  NOT NULL,
    currency_code       VARCHAR(3)     NOT NULL,
    trial_days          INT            NOT NULL DEFAULT 0,
    grace_days          INT            NOT NULL DEFAULT 0,
    status              VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    created_by          VARCHAR(36),
    created_at          DATETIME(6)    NOT NULL,
    updated_by          VARCHAR(36),
    updated_at          DATETIME(6),
    PRIMARY KEY (plan_id),
    UNIQUE KEY uk_billing_plan_code (plan_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE billing_plan_module (
    plan_module_id      CHAR(36)       NOT NULL,
    plan_id             CHAR(36)       NOT NULL,
    module_id           CHAR(36)       NOT NULL,
    PRIMARY KEY (plan_module_id),
    UNIQUE KEY uk_plan_module (plan_id, module_id),
    CONSTRAINT fk_bpm_plan FOREIGN KEY (plan_id) REFERENCES billing_plan (plan_id),
    CONSTRAINT fk_bpm_module FOREIGN KEY (module_id) REFERENCES platform_module (module_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE customer_subscription (
    subscription_id     CHAR(36)       NOT NULL,
    customer_id         CHAR(36)       NOT NULL,
    plan_id             CHAR(36)       NOT NULL,
    status              VARCHAR(20)    NOT NULL,
    start_date          DATE           NOT NULL,
    end_date            DATE,
    billing_cycle       VARCHAR(20)    NOT NULL,
    amount              DECIMAL(19,4)  NOT NULL,
    currency_code       VARCHAR(3)     NOT NULL,
    auto_renewal        TINYINT(1)     NOT NULL DEFAULT 0,
    trial_end_date      DATE,
    grace_end_date      DATE,
    cancelled_at        DATETIME(6),
    renewal_date        DATE,
    created_by          VARCHAR(36),
    created_at          DATETIME(6)    NOT NULL,
    updated_by          VARCHAR(36),
    updated_at          DATETIME(6),
    PRIMARY KEY (subscription_id),
    KEY idx_sub_customer_status (customer_id, status),
    CONSTRAINT fk_sub_customer FOREIGN KEY (customer_id) REFERENCES platform_customer (customer_id),
    CONSTRAINT fk_sub_plan FOREIGN KEY (plan_id) REFERENCES billing_plan (plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE billing_invoice (
    invoice_id          CHAR(36)       NOT NULL,
    invoice_number      VARCHAR(40)    NOT NULL,
    customer_id         CHAR(36)       NOT NULL,
    subscription_id     CHAR(36)       NOT NULL,
    invoice_date        DATE           NOT NULL,
    due_date            DATE           NOT NULL,
    currency_code       VARCHAR(3)     NOT NULL,
    subtotal            DECIMAL(19,4)  NOT NULL,
    tax_amount          DECIMAL(19,4)  NOT NULL DEFAULT 0,
    discount_amount     DECIMAL(19,4)  NOT NULL DEFAULT 0,
    total_amount        DECIMAL(19,4)  NOT NULL,
    payment_status      VARCHAR(20)    NOT NULL,
    payment_reference   VARCHAR(100),
    pdf_reference       VARCHAR(500),
    created_by          VARCHAR(36),
    created_at          DATETIME(6)    NOT NULL,
    updated_by          VARCHAR(36),
    updated_at          DATETIME(6),
    PRIMARY KEY (invoice_id),
    UNIQUE KEY uk_invoice_number (invoice_number),
    KEY idx_inv_customer (customer_id, payment_status),
    CONSTRAINT fk_inv_customer FOREIGN KEY (customer_id) REFERENCES platform_customer (customer_id),
    CONSTRAINT fk_inv_sub FOREIGN KEY (subscription_id) REFERENCES customer_subscription (subscription_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE billing_payment (
    payment_id          CHAR(36)       NOT NULL,
    invoice_id          CHAR(36)       NOT NULL,
    subscription_id     CHAR(36)       NOT NULL,
    customer_id         CHAR(36)       NOT NULL,
    gateway_code        VARCHAR(40)    NOT NULL,
    gateway_payment_ref VARCHAR(120),
    amount              DECIMAL(19,4)  NOT NULL,
    currency_code       VARCHAR(3)     NOT NULL,
    status              VARCHAR(30)    NOT NULL,
    initiated_at        DATETIME(6)    NOT NULL,
    paid_at             DATETIME(6),
    failure_reason      VARCHAR(500),
    created_by          VARCHAR(36),
    created_at          DATETIME(6)    NOT NULL,
    updated_by          VARCHAR(36),
    updated_at          DATETIME(6),
    PRIMARY KEY (payment_id),
    KEY idx_pay_invoice (invoice_id),
    KEY idx_pay_status (status),
    CONSTRAINT fk_pay_invoice FOREIGN KEY (invoice_id) REFERENCES billing_invoice (invoice_id),
    CONSTRAINT fk_pay_sub FOREIGN KEY (subscription_id) REFERENCES customer_subscription (subscription_id),
    CONSTRAINT fk_pay_customer FOREIGN KEY (customer_id) REFERENCES platform_customer (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE billing_webhook_event (
    webhook_event_id    CHAR(36)       NOT NULL,
    gateway_code        VARCHAR(40)    NOT NULL,
    gateway_event_id    VARCHAR(120)   NOT NULL,
    event_type          VARCHAR(80)    NOT NULL,
    payload_hash        VARCHAR(64)    NOT NULL,
    signature_valid     TINYINT(1)     NOT NULL,
    processed           TINYINT(1)     NOT NULL DEFAULT 0,
    processed_at        DATETIME(6),
    processing_result   VARCHAR(40),
    raw_payload         MEDIUMTEXT,
    received_at         DATETIME(6)    NOT NULL,
    PRIMARY KEY (webhook_event_id),
    UNIQUE KEY uk_gateway_event (gateway_code, gateway_event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE billing_refund (
    refund_id           CHAR(36)       NOT NULL,
    payment_id          CHAR(36)       NOT NULL,
    amount              DECIMAL(19,4)  NOT NULL,
    currency_code       VARCHAR(3)     NOT NULL,
    reason              VARCHAR(500),
    gateway_refund_ref  VARCHAR(120),
    status              VARCHAR(30)    NOT NULL,
    access_policy       VARCHAR(40)    NOT NULL DEFAULT 'KEEP_UNTIL_EXPIRY',
    refunded_at         DATETIME(6),
    created_by          VARCHAR(36),
    created_at          DATETIME(6)    NOT NULL,
    updated_by          VARCHAR(36),
    updated_at          DATETIME(6),
    PRIMARY KEY (refund_id),
    KEY idx_refund_payment (payment_id),
    CONSTRAINT fk_refund_payment FOREIGN KEY (payment_id) REFERENCES billing_payment (payment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE billing_chargeback (
    chargeback_id       CHAR(36)       NOT NULL,
    payment_id          CHAR(36)       NOT NULL,
    amount              DECIMAL(19,4)  NOT NULL,
    currency_code       VARCHAR(3)     NOT NULL,
    gateway_cb_ref      VARCHAR(120),
    status              VARCHAR(30)    NOT NULL,
    review_notes        VARCHAR(1000),
    access_action       VARCHAR(40),
    created_by          VARCHAR(36),
    created_at          DATETIME(6)    NOT NULL,
    updated_by          VARCHAR(36),
    updated_at          DATETIME(6),
    PRIMARY KEY (chargeback_id),
    KEY idx_cb_payment (payment_id),
    CONSTRAINT fk_cb_payment FOREIGN KEY (payment_id) REFERENCES billing_payment (payment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed catalog plans (modules referenced by code — not hard-coded in payment services)
INSERT INTO billing_plan (plan_id, plan_code, plan_name, description, billing_frequency, price, currency_code, trial_days, grace_days, status, created_at)
VALUES
  (UUID(), 'PC-MONTHLY', 'Product Costing Monthly', 'Single module PC', 'MONTHLY', 5000.0000, 'INR', 14, 7, 'ACTIVE', CURRENT_TIMESTAMP(6)),
  (UUID(), 'PC-VS-ANNUAL', 'PC + Vendor Selection Annual', 'Bundle PC and VS', 'ANNUAL', 100000.0000, 'INR', 14, 14, 'ACTIVE', CURRENT_TIMESTAMP(6));

INSERT INTO billing_plan_module (plan_module_id, plan_id, module_id)
SELECT UUID(), p.plan_id, m.module_id
FROM billing_plan p
CROSS JOIN platform_module m
WHERE p.plan_code = 'PC-MONTHLY' AND m.module_code = 'PRODUCT_COSTING';

INSERT INTO billing_plan_module (plan_module_id, plan_id, module_id)
SELECT UUID(), p.plan_id, m.module_id
FROM billing_plan p
CROSS JOIN platform_module m
WHERE p.plan_code = 'PC-VS-ANNUAL' AND m.module_code IN ('PRODUCT_COSTING', 'VENDOR_SELECTION');

-- Commercial metadata on module catalog (non-breaking)
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'platform_module' AND COLUMN_NAME = 'commercially_available');
SET @sql := IF(@c = 0,
  'ALTER TABLE platform_module ADD COLUMN commercially_available TINYINT(1) NOT NULL DEFAULT 1, ADD COLUMN subscription_eligible TINYINT(1) NOT NULL DEFAULT 1, ADD COLUMN catalog_version INT NOT NULL DEFAULT 1',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
