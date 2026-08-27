-- V20: Reporting & operations performance indexes (additive only)
-- Supports program-scoped reports, ERP staging validate/promote paths, and audit review.
-- No column changes; no data migration.

-- Program-scoped target variance aggregates (ReportService.targetVariance)
CREATE INDEX idx_target_variance_program
    ON target_cost_variance (program_id, exceeds_threshold, calculated_at);

-- ERP staging: program_code used at promote (BOM → vehicle program resolution)
CREATE INDEX idx_erp_part_program_code
    ON erp_part_staging (program_code, status);

CREATE INDEX idx_erp_bom_program_code
    ON erp_bom_staging (program_code, status);

-- ERP staging row status scans during validate
CREATE INDEX idx_erp_part_batch_status
    ON erp_part_staging (batch_id, status);

CREATE INDEX idx_erp_bom_batch_status
    ON erp_bom_staging (batch_id, status);

CREATE INDEX idx_erp_price_batch_status
    ON erp_material_price_staging (batch_id, status);

-- Master data ACTIVE version counts (reports master-data-health)
-- material/machine already have status indexes; labor may rely on full scan at scale
CREATE INDEX idx_labor_status
    ON labor_master (status);

-- Should-cost program roll-ups join via bom_node_id; composite with program already on bom_node
-- Ensure program_id + leaf lookups stay efficient for report subqueries
CREATE INDEX idx_bom_node_program_status
    ON bom_node (program_id, status);

-- Audit review: filter by action within a time window (admin Audit Log UI)
CREATE INDEX idx_audit_action_time
    ON audit_log (action, timestamp);
