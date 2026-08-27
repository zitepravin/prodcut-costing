-- V33: Stage A3 tenant isolation support
-- Link all existing active users to DEFAULT customer so single-tenant deployments keep working.
INSERT INTO customer_user_membership (membership_id, customer_id, user_id, status, created_at)
SELECT UUID(), c.customer_id, u.user_id, 'ACTIVE', CURRENT_TIMESTAMP(6)
FROM platform_customer c
CROSS JOIN app_user u
WHERE c.customer_code = 'DEFAULT'
  AND u.status = 'Active'
  AND NOT EXISTS (
      SELECT 1 FROM customer_user_membership m
      WHERE m.customer_id = c.customer_id AND m.user_id = u.user_id
  );

-- idempotent: idx_cum_customer_status
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'customer_user_membership' AND index_name = 'idx_cum_customer_status'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_cum_customer_status ON customer_user_membership (customer_id, status)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
-- idempotent: idx_cme_customer_enabled
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'customer_module_entitlement' AND index_name = 'idx_cme_customer_enabled'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_cme_customer_enabled ON customer_module_entitlement (customer_id, enabled)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
