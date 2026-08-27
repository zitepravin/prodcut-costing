-- Wave A: demo customers for independent module subscription matrices.
-- Assign users via Platform CAM. No business data seeded.

INSERT INTO platform_customer (customer_id, customer_code, customer_name, status, created_at)
SELECT UUID(), 'DEMO_PURCHASE_ONLY', 'Demo Purchase Only', 'ACTIVE', CURRENT_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM platform_customer WHERE customer_code = 'DEMO_PURCHASE_ONLY');

INSERT INTO customer_module_entitlement (entitlement_id, customer_id, module_id, enabled, created_at)
SELECT UUID(), c.customer_id, m.module_id, TRUE, CURRENT_TIMESTAMP(6)
FROM platform_customer c
INNER JOIN platform_module m ON m.module_code = 'PURCHASE'
WHERE c.customer_code = 'DEMO_PURCHASE_ONLY'
  AND NOT EXISTS (
      SELECT 1 FROM customer_module_entitlement e
      WHERE e.customer_id = c.customer_id AND e.module_id = m.module_id
  );

INSERT INTO platform_customer (customer_id, customer_code, customer_name, status, created_at)
SELECT UUID(), 'DEMO_COSTING_ONLY', 'Demo Costing Only', 'ACTIVE', CURRENT_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM platform_customer WHERE customer_code = 'DEMO_COSTING_ONLY');

INSERT INTO customer_module_entitlement (entitlement_id, customer_id, module_id, enabled, created_at)
SELECT UUID(), c.customer_id, m.module_id, TRUE, CURRENT_TIMESTAMP(6)
FROM platform_customer c
INNER JOIN platform_module m ON m.module_code = 'PRODUCT_COSTING'
WHERE c.customer_code = 'DEMO_COSTING_ONLY'
  AND NOT EXISTS (
      SELECT 1 FROM customer_module_entitlement e
      WHERE e.customer_id = c.customer_id AND e.module_id = m.module_id
  );

INSERT INTO platform_customer (customer_id, customer_code, customer_name, status, created_at)
SELECT UUID(), 'DEMO_VS_ONLY', 'Demo Vendor Selection Only', 'ACTIVE', CURRENT_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM platform_customer WHERE customer_code = 'DEMO_VS_ONLY');

INSERT INTO customer_module_entitlement (entitlement_id, customer_id, module_id, enabled, created_at)
SELECT UUID(), c.customer_id, m.module_id, TRUE, CURRENT_TIMESTAMP(6)
FROM platform_customer c
INNER JOIN platform_module m ON m.module_code = 'VENDOR_SELECTION'
WHERE c.customer_code = 'DEMO_VS_ONLY'
  AND NOT EXISTS (
      SELECT 1 FROM customer_module_entitlement e
      WHERE e.customer_id = c.customer_id AND e.module_id = m.module_id
  );
