-- V62: Vendor Selection in-app notification coverage for qualification lifecycle + operation blocks
-- notification_type remains VARCHAR; seed reference rows optional for ops documentation
CREATE TABLE IF NOT EXISTS vendor_selection_notification_catalog (
    catalog_id          VARCHAR(36)  NOT NULL,
    notification_type   VARCHAR(50)  NOT NULL,
    description         VARCHAR(500) NOT NULL,
    auto_approve_forbidden BOOLEAN NOT NULL DEFAULT TRUE,
    active_flag         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          DATETIME(6)  NOT NULL,
    PRIMARY KEY (catalog_id),
    UNIQUE KEY uk_vsnc_type (notification_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO vendor_selection_notification_catalog (catalog_id, notification_type, description, auto_approve_forbidden, active_flag, created_at)
SELECT UUID(), 'VENDOR_QUALIFICATION_APPROVED', 'Supplier lifecycle moved to APPROVED (human-controlled)', TRUE, TRUE, UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM vendor_selection_notification_catalog WHERE notification_type = 'VENDOR_QUALIFICATION_APPROVED');

INSERT INTO vendor_selection_notification_catalog (catalog_id, notification_type, description, auto_approve_forbidden, active_flag, created_at)
SELECT UUID(), 'VENDOR_QUALIFICATION_REJECTED', 'Supplier lifecycle moved to REJECTED', TRUE, TRUE, UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM vendor_selection_notification_catalog WHERE notification_type = 'VENDOR_QUALIFICATION_REJECTED');

INSERT INTO vendor_selection_notification_catalog (catalog_id, notification_type, description, auto_approve_forbidden, active_flag, created_at)
SELECT UUID(), 'VENDOR_QUALIFICATION_BLOCKED', 'Supplier lifecycle moved to BLOCKED', TRUE, TRUE, UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM vendor_selection_notification_catalog WHERE notification_type = 'VENDOR_QUALIFICATION_BLOCKED');

INSERT INTO vendor_selection_notification_catalog (catalog_id, notification_type, description, auto_approve_forbidden, active_flag, created_at)
SELECT UUID(), 'VENDOR_LIFECYCLE_OPERATION_BLOCKED', 'Scorecard/TCO blocked due to BLOCKED/SUSPENDED lifecycle', TRUE, TRUE, UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM vendor_selection_notification_catalog WHERE notification_type = 'VENDOR_LIFECYCLE_OPERATION_BLOCKED');

INSERT INTO vendor_selection_notification_catalog (catalog_id, notification_type, description, auto_approve_forbidden, active_flag, created_at)
SELECT UUID(), 'VENDOR_QUALIFICATION_TRANSITION', 'Other qualification lifecycle transitions', TRUE, TRUE, UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM vendor_selection_notification_catalog WHERE notification_type = 'VENDOR_QUALIFICATION_TRANSITION');
