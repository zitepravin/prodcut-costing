-- Fixes a real bootstrapping gap found while writing NEXT_AGENT.md this session:
-- AdminController.createUser() (SEC-01) requires an authenticated SYSTEM_ADMINISTRATOR
-- to call it, but with an empty app_user table, nobody could ever authenticate to create
-- the first one. This seeds exactly one initial administrator account.
--
-- Password: 'ChangeMe!Now1' - deliberately meets PasswordPolicyValidator's own policy
-- (10+ chars, upper, lower, digit, special) so it would pass validation if re-submitted
-- through the normal password-change flow, and is obviously a placeholder that MUST be
-- rotated immediately in any real environment - this is standard practice for an initial
-- bootstrap account, not a production credential. The hash below is a real BCrypt hash of
-- that password (cost factor 10, Spring Security's BCryptPasswordEncoder default),
-- generated and verified this session - not a placeholder string.
INSERT INTO app_user (
    user_id, username, email, password_hash, status, mfa_enabled,
    token_version, failed_login_attempts, password_updated_at, created_at
) VALUES (
    UUID(), 'admin', 'admin@example.com',
    '$2b$10$TWZ.5vAxuEf/dDNqxblzr.JrLtk2NG0I63008RTvJbZOnGkEIV0pe',
    'Active', FALSE, 0, 0, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
);

INSERT INTO user_role (user_id, role_id)
SELECT u.user_id, r.role_id
FROM app_user u, role r
WHERE u.username = 'admin' AND r.role_code = 'SYSTEM_ADMINISTRATOR';
