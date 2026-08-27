-- V166: Dedicated demo login per Dashboard business persona (16 roles).
-- Password for all demo users: ChangeMe!Now1
-- BCrypt (same as V9/V18): $2b$10$TWZ.5vAxuEf/dDNqxblzr.JrLtk2NG0I63008RTvJbZOnGkEIV0pe
--
-- Reused from V18 (do not duplicate):
--   demo.engineer  COSTING_ENGINEER
--   demo.manager   COSTING_MANAGER
--   demo.buyer     PURCHASING_BUYER
--   demo.master    MASTER_DATA_ADMIN
-- Bootstrap admin (V9) remains SYSTEM_ADMINISTRATOR; demo.systemadmin is the
-- dedicated Dashboard test account for that persona.
-- APPROVAL_OVERRIDE is not a Dashboard persona — no demo user created.
-- Each new user gets exactly one role. Tenant: DEFAULT membership (V33 pattern).

-- ---------- users ----------
INSERT INTO app_user (
    user_id, username, email, display_name, password_hash, status, mfa_enabled,
    token_version, failed_login_attempts, password_updated_at, created_at
)
SELECT 'f1000000-0000-4000-8000-000000000005', 'demo.systemadmin', 'systemadmin@demo.auto',
       'Demo System Administrator',
       '$2b$10$TWZ.5vAxuEf/dDNqxblzr.JrLtk2NG0I63008RTvJbZOnGkEIV0pe',
       'Active', FALSE, 0, 0, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'demo.systemadmin');

INSERT INTO app_user (
    user_id, username, email, display_name, password_hash, status, mfa_enabled,
    token_version, failed_login_attempts, password_updated_at, created_at
)
SELECT 'f1000000-0000-4000-8000-000000000006', 'demo.finance', 'finance@demo.auto',
       'Demo Finance Controller',
       '$2b$10$TWZ.5vAxuEf/dDNqxblzr.JrLtk2NG0I63008RTvJbZOnGkEIV0pe',
       'Active', FALSE, 0, 0, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'demo.finance');

INSERT INTO app_user (
    user_id, username, email, display_name, password_hash, status, mfa_enabled,
    token_version, failed_login_attempts, password_updated_at, created_at
)
SELECT 'f1000000-0000-4000-8000-000000000007', 'demo.sourcing', 'sourcing@demo.auto',
       'Demo Sourcing Lead',
       '$2b$10$TWZ.5vAxuEf/dDNqxblzr.JrLtk2NG0I63008RTvJbZOnGkEIV0pe',
       'Active', FALSE, 0, 0, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'demo.sourcing');

INSERT INTO app_user (
    user_id, username, email, display_name, password_hash, status, mfa_enabled,
    token_version, failed_login_attempts, password_updated_at, created_at
)
SELECT 'f1000000-0000-4000-8000-000000000008', 'demo.programcost', 'programcost@demo.auto',
       'Demo Program Cost Manager',
       '$2b$10$TWZ.5vAxuEf/dDNqxblzr.JrLtk2NG0I63008RTvJbZOnGkEIV0pe',
       'Active', FALSE, 0, 0, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'demo.programcost');

INSERT INTO app_user (
    user_id, username, email, display_name, password_hash, status, mfa_enabled,
    token_version, failed_login_attempts, password_updated_at, created_at
)
SELECT 'f1000000-0000-4000-8000-000000000009', 'demo.process', 'process@demo.auto',
       'Demo Process Engineer',
       '$2b$10$TWZ.5vAxuEf/dDNqxblzr.JrLtk2NG0I63008RTvJbZOnGkEIV0pe',
       'Active', FALSE, 0, 0, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'demo.process');

INSERT INTO app_user (
    user_id, username, email, display_name, password_hash, status, mfa_enabled,
    token_version, failed_login_attempts, password_updated_at, created_at
)
SELECT 'f1000000-0000-4000-8000-00000000000a', 'demo.tooling', 'tooling@demo.auto',
       'Demo Tooling Engineer',
       '$2b$10$TWZ.5vAxuEf/dDNqxblzr.JrLtk2NG0I63008RTvJbZOnGkEIV0pe',
       'Active', FALSE, 0, 0, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'demo.tooling');

INSERT INTO app_user (
    user_id, username, email, display_name, password_hash, status, mfa_enabled,
    token_version, failed_login_attempts, password_updated_at, created_at
)
SELECT 'f1000000-0000-4000-8000-00000000000b', 'demo.materialbuyer', 'materialbuyer@demo.auto',
       'Demo Commodity / Material Buyer',
       '$2b$10$TWZ.5vAxuEf/dDNqxblzr.JrLtk2NG0I63008RTvJbZOnGkEIV0pe',
       'Active', FALSE, 0, 0, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'demo.materialbuyer');

INSERT INTO app_user (
    user_id, username, email, display_name, password_hash, status, mfa_enabled,
    token_version, failed_login_attempts, password_updated_at, created_at
)
SELECT 'f1000000-0000-4000-8000-00000000000c', 'demo.industrial', 'industrial@demo.auto',
       'Demo Industrial Engineer',
       '$2b$10$TWZ.5vAxuEf/dDNqxblzr.JrLtk2NG0I63008RTvJbZOnGkEIV0pe',
       'Active', FALSE, 0, 0, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'demo.industrial');

INSERT INTO app_user (
    user_id, username, email, display_name, password_hash, status, mfa_enabled,
    token_version, failed_login_attempts, password_updated_at, created_at
)
SELECT 'f1000000-0000-4000-8000-00000000000d', 'demo.productcost', 'productcost@demo.auto',
       'Demo Product Cost Engineer',
       '$2b$10$TWZ.5vAxuEf/dDNqxblzr.JrLtk2NG0I63008RTvJbZOnGkEIV0pe',
       'Active', FALSE, 0, 0, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'demo.productcost');

INSERT INTO app_user (
    user_id, username, email, display_name, password_hash, status, mfa_enabled,
    token_version, failed_login_attempts, password_updated_at, created_at
)
SELECT 'f1000000-0000-4000-8000-00000000000e', 'demo.purchasemanager', 'purchasemanager@demo.auto',
       'Demo Purchasing Manager',
       '$2b$10$TWZ.5vAxuEf/dDNqxblzr.JrLtk2NG0I63008RTvJbZOnGkEIV0pe',
       'Active', FALSE, 0, 0, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'demo.purchasemanager');

INSERT INTO app_user (
    user_id, username, email, display_name, password_hash, status, mfa_enabled,
    token_version, failed_login_attempts, password_updated_at, created_at
)
SELECT 'f1000000-0000-4000-8000-00000000000f', 'demo.platformowner', 'platformowner@demo.auto',
       'Demo Platform Owner',
       '$2b$10$TWZ.5vAxuEf/dDNqxblzr.JrLtk2NG0I63008RTvJbZOnGkEIV0pe',
       'Active', FALSE, 0, 0, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'demo.platformowner');

INSERT INTO app_user (
    user_id, username, email, display_name, password_hash, status, mfa_enabled,
    token_version, failed_login_attempts, password_updated_at, created_at
)
SELECT 'f1000000-0000-4000-8000-000000000010', 'demo.customeradmin', 'customeradmin@demo.auto',
       'Demo Customer Admin',
       '$2b$10$TWZ.5vAxuEf/dDNqxblzr.JrLtk2NG0I63008RTvJbZOnGkEIV0pe',
       'Active', FALSE, 0, 0, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'demo.customeradmin');

-- ---------- exactly one role each ----------
INSERT IGNORE INTO user_role (user_id, role_id)
SELECT u.user_id, r.role_id FROM app_user u, role r
WHERE u.username = 'demo.systemadmin' AND r.role_code = 'SYSTEM_ADMINISTRATOR';

INSERT IGNORE INTO user_role (user_id, role_id)
SELECT u.user_id, r.role_id FROM app_user u, role r
WHERE u.username = 'demo.finance' AND r.role_code = 'FINANCE_CONTROLLER';

INSERT IGNORE INTO user_role (user_id, role_id)
SELECT u.user_id, r.role_id FROM app_user u, role r
WHERE u.username = 'demo.sourcing' AND r.role_code = 'SOURCING_LEAD';

INSERT IGNORE INTO user_role (user_id, role_id)
SELECT u.user_id, r.role_id FROM app_user u, role r
WHERE u.username = 'demo.programcost' AND r.role_code = 'PROGRAM_COST_MANAGER';

INSERT IGNORE INTO user_role (user_id, role_id)
SELECT u.user_id, r.role_id FROM app_user u, role r
WHERE u.username = 'demo.process' AND r.role_code = 'PROCESS_ENGINEER';

INSERT IGNORE INTO user_role (user_id, role_id)
SELECT u.user_id, r.role_id FROM app_user u, role r
WHERE u.username = 'demo.tooling' AND r.role_code = 'TOOLING_ENGINEER';

INSERT IGNORE INTO user_role (user_id, role_id)
SELECT u.user_id, r.role_id FROM app_user u, role r
WHERE u.username = 'demo.materialbuyer' AND r.role_code = 'COMMODITY_MATERIAL_BUYER';

INSERT IGNORE INTO user_role (user_id, role_id)
SELECT u.user_id, r.role_id FROM app_user u, role r
WHERE u.username = 'demo.industrial' AND r.role_code = 'INDUSTRIAL_ENGINEER';

INSERT IGNORE INTO user_role (user_id, role_id)
SELECT u.user_id, r.role_id FROM app_user u, role r
WHERE u.username = 'demo.productcost' AND r.role_code = 'PRODUCT_COST_ENGINEER';

INSERT IGNORE INTO user_role (user_id, role_id)
SELECT u.user_id, r.role_id FROM app_user u, role r
WHERE u.username = 'demo.purchasemanager' AND r.role_code = 'PURCHASING_MANAGER';

INSERT IGNORE INTO user_role (user_id, role_id)
SELECT u.user_id, r.role_id FROM app_user u, role r
WHERE u.username = 'demo.platformowner' AND r.role_code = 'PLATFORM_OWNER';

INSERT IGNORE INTO user_role (user_id, role_id)
SELECT u.user_id, r.role_id FROM app_user u, role r
WHERE u.username = 'demo.customeradmin' AND r.role_code = 'CUSTOMER_ADMIN';

-- ---------- DEFAULT tenant membership (required for CustomerContext) ----------
INSERT INTO customer_user_membership (membership_id, customer_id, user_id, status, created_at)
SELECT UUID(), c.customer_id, u.user_id, 'ACTIVE', CURRENT_TIMESTAMP(6)
FROM platform_customer c
CROSS JOIN app_user u
WHERE c.customer_code = 'DEFAULT'
  AND u.username IN (
      'demo.systemadmin', 'demo.finance', 'demo.sourcing', 'demo.programcost',
      'demo.process', 'demo.tooling', 'demo.materialbuyer', 'demo.industrial',
      'demo.productcost', 'demo.purchasemanager', 'demo.platformowner', 'demo.customeradmin',
      'demo.engineer', 'demo.manager', 'demo.buyer', 'demo.master'
  )
  AND NOT EXISTS (
      SELECT 1 FROM customer_user_membership m
      WHERE m.customer_id = c.customer_id AND m.user_id = u.user_id
  );
