-- V3: BOM & Product Structure Management module (M02) - DATABASE_SCHEMA.md Section 3.
-- bom_node is self-referencing to model unlimited BOM depth (BOM-01); a WITH RECURSIVE
-- CTE (MySQL 8) is used for tree/subtree queries rather than a closure table for now -
-- revisit with a closure table only if roll-up read performance at 20,000 nodes requires
-- it under the 30s SLA (TDD Section 4.8 / Section 9).

CREATE TABLE bom_node (
    bom_node_id             CHAR(36)      NOT NULL PRIMARY KEY,
    parent_node_id          CHAR(36)      NULL,
    program_id              CHAR(36)      NOT NULL,
    part_number              VARCHAR(50)   NOT NULL,
    revision                 VARCHAR(20),
    quantity_per             NUMERIC(18,6),
    uom                      VARCHAR(10),
    make_buy_flag            VARCHAR(10),
    ecn_reference             VARCHAR(50),
    net_weight                NUMERIC(18,6),
    scrap_pct                 NUMERIC(9,6),
    status                    VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    what_if_branch_flag        BOOLEAN       NOT NULL DEFAULT FALSE,
    rolled_up_cost             NUMERIC(18,6),
    manual_cost_override       NUMERIC(18,6),
    override_flag              BOOLEAN       NOT NULL DEFAULT FALSE,
    override_justification     VARCHAR(500),
    created_by                 VARCHAR(36),
    created_at                 DATETIME(6)   NOT NULL,
    updated_by                 VARCHAR(36),
    updated_at                 DATETIME(6),
    CONSTRAINT fk_bom_node_parent FOREIGN KEY (parent_node_id) REFERENCES bom_node (bom_node_id),
    CONSTRAINT fk_bom_node_program FOREIGN KEY (program_id) REFERENCES vehicle_program (program_id)
) ENGINE = InnoDB;

-- Tree traversal indexes: every subtree/full-tree query in BomNodeRepository filters or
-- joins on these two columns (TDD Section 6.3's "batch-load the subtree in one query").
-- idempotent: idx_bom_node_parent
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'bom_node' AND index_name = 'idx_bom_node_parent'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_bom_node_parent ON bom_node (parent_node_id)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
-- idempotent: idx_bom_node_program
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'bom_node' AND index_name = 'idx_bom_node_program'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_bom_node_program ON bom_node (program_id)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
-- idempotent: idx_bom_node_program_parent
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'bom_node' AND index_name = 'idx_bom_node_program_parent'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_bom_node_program_parent ON bom_node (program_id, parent_node_id)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
