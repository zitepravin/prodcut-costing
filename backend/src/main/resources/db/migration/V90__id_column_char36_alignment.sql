-- V90: Align UUID ID columns to CHAR(36) utf8mb4_unicode_ci for Hibernate ddl-auto=validate
-- and MySQL FK type consistency (parent supplier_master.supplier_id is CHAR(36)).
-- Scope: cost_version_record + core Vendor Selection child tables with VARCHAR(36) IDs
-- referencing supplier_master. Audit columns created_by/updated_by left as VARCHAR(36)
-- (no FK). FOREIGN_KEY_CHECKS briefly disabled for in-place MODIFY (MySQL 8).

SET FOREIGN_KEY_CHECKS = 0;

-- M15 version control
ALTER TABLE cost_version_record
    MODIFY version_id CHAR(36) NOT NULL,
    MODIFY entity_id CHAR(36) NOT NULL;

-- Capability / eligibility / quality / delivery / warranty / guarantee terms (V34–V35)
ALTER TABLE supplier_capability
    MODIFY capability_id CHAR(36) NOT NULL,
    MODIFY supplier_id CHAR(36) NOT NULL;

ALTER TABLE supplier_eligibility_result
    MODIFY result_id CHAR(36) NOT NULL,
    MODIFY supplier_id CHAR(36) NOT NULL;

ALTER TABLE supplier_quality_metric
    MODIFY metric_id CHAR(36) NOT NULL,
    MODIFY supplier_id CHAR(36) NOT NULL;

ALTER TABLE supplier_delivery_metric
    MODIFY metric_id CHAR(36) NOT NULL,
    MODIFY supplier_id CHAR(36) NOT NULL;

ALTER TABLE supplier_warranty_term
    MODIFY warranty_id CHAR(36) NOT NULL,
    MODIFY supplier_id CHAR(36) NOT NULL;

ALTER TABLE supplier_guarantee_term
    MODIFY guarantee_id CHAR(36) NOT NULL,
    MODIFY supplier_id CHAR(36) NOT NULL;

-- Evidence depth (V64–V69)
ALTER TABLE supplier_warranty_claim
    MODIFY claim_id CHAR(36) NOT NULL,
    MODIFY supplier_id CHAR(36) NOT NULL,
    MODIFY warranty_id CHAR(36) NULL;

ALTER TABLE supplier_guarantee_event
    MODIFY event_id CHAR(36) NOT NULL,
    MODIFY supplier_id CHAR(36) NOT NULL,
    MODIFY guarantee_id CHAR(36) NULL;

ALTER TABLE supplier_after_sales_metric
    MODIFY metric_id CHAR(36) NOT NULL,
    MODIFY supplier_id CHAR(36) NOT NULL;

ALTER TABLE supplier_performance_feedback
    MODIFY feedback_id CHAR(36) NOT NULL,
    MODIFY supplier_id CHAR(36) NOT NULL,
    MODIFY history_metric_id CHAR(36) NULL;

SET FOREIGN_KEY_CHECKS = 1;
