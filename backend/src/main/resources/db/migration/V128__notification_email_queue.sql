-- Wave F: email templates, delivery queue, user preferences
-- Safe placeholders only; no code execution in templates.

CREATE TABLE notification_template (
    template_id       CHAR(36)       NOT NULL,
    template_code     VARCHAR(80)    NOT NULL,
    module_code       VARCHAR(40)    NULL,
    event_type        VARCHAR(40)    NOT NULL,
    subject_template  VARCHAR(500)   NOT NULL,
    body_template     VARCHAR(4000)  NOT NULL,
    recipient_type    VARCHAR(40)    NULL,
    language_code     VARCHAR(10)    NOT NULL DEFAULT 'en',
    active_flag       TINYINT(1)     NOT NULL DEFAULT 1,
    created_by        VARCHAR(36)    NULL,
    created_at        TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by        VARCHAR(36)    NULL,
    updated_at        TIMESTAMP(6)   NULL,
    PRIMARY KEY (template_id),
    UNIQUE KEY uk_template_code_lang (template_code, language_code),
    KEY idx_template_event (event_type, active_flag)
);

CREATE TABLE notification_delivery (
    delivery_id           CHAR(36)       NOT NULL,
    notification_id       CHAR(36)       NULL,
    idempotency_key       VARCHAR(120)   NOT NULL,
    recipient_user_id     CHAR(36)       NOT NULL,
    recipient_email       VARCHAR(150)   NULL,
    template_code         VARCHAR(80)    NULL,
    event_type            VARCHAR(40)    NOT NULL,
    subject_rendered      VARCHAR(500)   NULL,
    body_rendered         VARCHAR(4000)  NULL,
    status                VARCHAR(20)    NOT NULL DEFAULT 'QUEUED',
    attempt_count         INT            NOT NULL DEFAULT 0,
    max_attempts          INT            NOT NULL DEFAULT 3,
    last_error            VARCHAR(1000)  NULL,
    correlation_id        VARCHAR(64)    NULL,
    scheduled_at          TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    processed_at          TIMESTAMP(6)   NULL,
    created_at            TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at            TIMESTAMP(6)   NULL,
    PRIMARY KEY (delivery_id),
    UNIQUE KEY uk_delivery_idempotency (idempotency_key),
    KEY idx_delivery_status_sched (status, scheduled_at),
    KEY idx_delivery_notification (notification_id)
);

CREATE TABLE notification_preference (
    preference_id       CHAR(36)       NOT NULL,
    user_id             CHAR(36)       NOT NULL,
    customer_id         CHAR(36)       NULL,
    module_code         VARCHAR(40)    NOT NULL,
    event_type          VARCHAR(40)    NOT NULL,
    email_enabled       TINYINT(1)     NOT NULL DEFAULT 1,
    in_app_enabled      TINYINT(1)     NOT NULL DEFAULT 1,
    created_at          TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6)   NULL,
    PRIMARY KEY (preference_id),
    UNIQUE KEY uk_pref_user_module_event (user_id, module_code, event_type),
    KEY idx_pref_user (user_id)
);

-- Seed default templates (platform-wide)
INSERT INTO notification_template (template_id, template_code, module_code, event_type, subject_template, body_template, recipient_type, language_code, active_flag)
VALUES
('00000000-0000-4000-8000-000000000201', 'PR_SUBMITTED_EN', 'PURCHASE', 'PURCHASE_REQUEST_SUBMITTED',
 'Purchase request {{documentNumber}} submitted',
 'Hello {{userName}},\n\nPurchase request {{documentNumber}} was submitted.\nStatus: {{status}}\n\nSign in to review.\n',
 'REQUESTER', 'en', 1),
('00000000-0000-4000-8000-000000000202', 'PO_APPROVAL_EN', 'PURCHASE', 'PURCHASE_ORDER_APPROVAL_REQUIRED',
 'Purchase order {{documentNumber}} needs approval',
 'Hello {{userName}},\n\nPO {{documentNumber}} requires approval.\nStatus: {{status}}\n\nSign in to decide.\n',
 'APPROVER', 'en', 1),
('00000000-0000-4000-8000-000000000203', 'RFQ_CREATED_EN', 'PRODUCT_COSTING', 'RFQ_CREATED',
 'RFQ {{documentNumber}} created',
 'Hello {{userName}},\n\nRFQ {{documentNumber}} was created.\nStatus: {{status}}\n\nSign in to continue.\n',
 'BUYER', 'en', 1);
