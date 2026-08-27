-- Purchase-only tenants: PR need not be tied to Product Costing BOM/program.
ALTER TABLE purchase_request
    MODIFY COLUMN program_id CHAR(36) NULL,
    MODIFY COLUMN bom_node_id CHAR(36) NULL;

CREATE INDEX idx_pr_customer_created ON purchase_request (customer_id, created_at);
