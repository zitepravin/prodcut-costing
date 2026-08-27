-- V164: Dashboard operational KPI indexes + missing business roles used by role-wise personas.
-- Does not invent data. Aggregates read existing rfq_event, purchase_order, process_*, tooling_record, material_master.

INSERT INTO role (role_id, role_code, description, is_system_role)
SELECT UUID(), 'COMMODITY_MATERIAL_BUYER', 'Material / commodity sourcing and material cost control', TRUE
WHERE NOT EXISTS (SELECT 1 FROM role WHERE role_code = 'COMMODITY_MATERIAL_BUYER');

INSERT INTO role (role_id, role_code, description, is_system_role)
SELECT UUID(), 'INDUSTRIAL_ENGINEER', 'Process routing, cycle time and line balancing', TRUE
WHERE NOT EXISTS (SELECT 1 FROM role WHERE role_code = 'INDUSTRIAL_ENGINEER');

INSERT INTO role (role_id, role_code, description, is_system_role)
SELECT UUID(), 'PRODUCT_COST_ENGINEER', 'Should-cost build and supplier quote challenge', TRUE
WHERE NOT EXISTS (SELECT 1 FROM role WHERE role_code = 'PRODUCT_COST_ENGINEER');

-- Idempotent supporting indexes for tenant-scoped dashboard counts
SET @idx := (SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'rfq_event' AND index_name = 'idx_rfq_cust_status_due');
SET @sql := IF(@idx = 0, 'CREATE INDEX idx_rfq_cust_status_due ON rfq_event (customer_id, status, due_date)', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @idx := (SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'purchase_order' AND index_name = 'idx_po_cust_status_due');
SET @sql := IF(@idx = 0, 'CREATE INDEX idx_po_cust_status_due ON purchase_order (customer_id, status, required_delivery_date)', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @idx := (SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'purchase_order' AND index_name = 'idx_po_cust_buyer_status');
SET @sql := IF(@idx = 0, 'CREATE INDEX idx_po_cust_buyer_status ON purchase_order (customer_id, buyer_user_id, status)', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @idx := (SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'purchase_request' AND index_name = 'idx_pr_cust_status');
SET @sql := IF(@idx = 0, 'CREATE INDEX idx_pr_cust_status ON purchase_request (customer_id, status)', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @idx := (SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'process_routing' AND index_name = 'idx_routing_cust_status');
SET @sql := IF(@idx = 0, 'CREATE INDEX idx_routing_cust_status ON process_routing (customer_id, status)', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @idx := (SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'process_operation' AND index_name = 'idx_op_cust_machine_labor');
SET @sql := IF(@idx = 0, 'CREATE INDEX idx_op_cust_machine_labor ON process_operation (customer_id, machine_code, labor_code)', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @idx := (SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'tooling_record' AND index_name = 'idx_tooling_cust_asset');
SET @sql := IF(@idx = 0, 'CREATE INDEX idx_tooling_cust_asset ON tooling_record (customer_id, asset_status)', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @idx := (SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'material_master' AND index_name = 'idx_mat_cust_grade_price');
SET @sql := IF(@idx = 0, 'CREATE INDEX idx_mat_cust_grade_price ON material_master (customer_id, grade, base_price)', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
