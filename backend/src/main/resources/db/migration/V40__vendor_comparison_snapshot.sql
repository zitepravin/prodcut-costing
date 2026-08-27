-- V40: Side-by-side supplier comparison snapshots (decision support, not approval)
CREATE TABLE vendor_comparison (
    comparison_id       VARCHAR(36)  NOT NULL,
    bom_node_id CHAR(36),
    program_id CHAR(36),
    currency_code       VARCHAR(3),
    supplier_ids_csv    VARCHAR(2000) NOT NULL,
    comparison_json     LONGTEXT     NOT NULL,
    notes               VARCHAR(1000),
    created_by          VARCHAR(36),
    created_at          DATETIME(6)  NOT NULL,
    updated_by          VARCHAR(36),
    updated_at          DATETIME(6),
    PRIMARY KEY (comparison_id),
    INDEX idx_vc_bom (bom_node_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
