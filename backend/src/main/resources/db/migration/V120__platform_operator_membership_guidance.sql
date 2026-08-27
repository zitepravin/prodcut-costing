-- Production guidance (no destructive change):
-- Platform operators (SYSTEM_ADMINISTRATOR / PLATFORM_OWNER) should NOT hold ACTIVE
-- membership on customer tenants by default. V33 linked all users to DEFAULT for
-- single-tenant convenience. For multi-tenant confidentiality:
--   1) Keep platform operators out of customer_user_membership (ACTIVE).
--   2) Use support_access_session for time-limited investigation.
-- This migration is intentionally a no-op DDL marker so operators see the policy in Flyway history.
SELECT 1;
