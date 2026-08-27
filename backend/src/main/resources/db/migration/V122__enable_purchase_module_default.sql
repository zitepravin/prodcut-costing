-- Enable PURCHASE (and ORDER_TRACKING) for DEFAULT customer so demo/live-gate can use P3-01.
-- Module codes already exist from V32; entitlement was only PRODUCT_COSTING + VENDOR_SELECTION.

INSERT INTO customer_module_entitlement (entitlement_id, customer_id, module_id, enabled, created_at)
SELECT UUID(), c.customer_id, m.module_id, TRUE, CURRENT_TIMESTAMP(6)
FROM platform_customer c
CROSS JOIN platform_module m
WHERE c.customer_code = 'DEFAULT'
  AND m.module_code IN ('PURCHASE', 'ORDER_TRACKING')
  AND NOT EXISTS (
      SELECT 1 FROM customer_module_entitlement e
      WHERE e.customer_id = c.customer_id AND e.module_id = m.module_id
  );

-- If rows exist but disabled, turn on
UPDATE customer_module_entitlement e
INNER JOIN platform_customer c ON c.customer_id = e.customer_id AND c.customer_code = 'DEFAULT'
INNER JOIN platform_module m ON m.module_id = e.module_id AND m.module_code IN ('PURCHASE', 'ORDER_TRACKING')
SET e.enabled = TRUE,
    e.updated_at = CURRENT_TIMESTAMP(6);
