-- M03 Raw Material Cost Management (SRS 4.3, TDD 5.03, Test Plan 7.3)
-- MAT-01..MAT-08. Extends material_master (index-linked pricing inputs) and bom_node
-- (material linkage, so MAT-05's "recalculate dependent BOM costs" has something to
-- find), and introduces the two new tables this module owns.

-- MAT-02: index-linked pricing formula inputs. A material can be priced either as a
-- flat contract rate (the existing `rate` column, MDM's default) or as index-linked
-- (basePrice + commodity index value at the effective date + alloy surcharge). Both
-- paths coexist on the same row so MDM-02 versioning continues to apply uniformly -
-- an index-linked material's `rate` column is still populated (kept in sync with the
-- fixed-price contract rate at creation time) but MaterialCostEngine ignores it in
-- favor of the index formula when index_linked_flag = TRUE.
ALTER TABLE material_master
    ADD COLUMN index_linked_flag       BOOLEAN       NOT NULL DEFAULT FALSE,
    ADD COLUMN index_code              VARCHAR(30),
    ADD COLUMN base_price              NUMERIC(18,6),
    ADD COLUMN alloy_surcharge_per_unit NUMERIC(18,6);

-- BOM-07 / MAT-05: a BOM leaf node needs to know which material it consumes so that
-- (a) BomRollupEngine.LeafUnitCostResolver can resolve a real material cost instead of
-- the M02-session stub, and (b) a material rate publish event can find every BOM node
-- that must be recalculated. Nullable - assembly-level / 'Make' nodes with no direct
-- raw-material consumption of their own leave this null.
ALTER TABLE bom_node
    ADD COLUMN material_code VARCHAR(50);

-- idempotent: idx_bom_node_material_code
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'bom_node' AND index_name = 'idx_bom_node_material_code'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_bom_node_material_code ON bom_node (material_code)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- MAT-02: dated commodity index values (e.g. LME aluminum, HRC steel index), append-only
-- per index code - a new row per publish, never an in-place update, mirroring the
-- MDM-02 versioning convention used across the rest of master data.
CREATE TABLE commodity_index_value (
    index_value_id  CHAR(36)      NOT NULL PRIMARY KEY,
    index_code      VARCHAR(30)   NOT NULL,
    index_value     NUMERIC(18,6) NOT NULL,
    effective_date  DATE          NOT NULL,
    source          VARCHAR(100),
    created_by      VARCHAR(36),
    created_at      DATETIME(6)   NOT NULL,
    updated_by      VARCHAR(36),
    updated_at      DATETIME(6)
) ENGINE = InnoDB;

CREATE UNIQUE INDEX uk_commodity_index_code_date ON commodity_index_value (index_code, effective_date);

-- MAT-01/02/03/06: the persisted output of a single material cost calculation. This is
-- deliberately NOT `cost_record` (TDD Section 4.3 / DATABASE_SCHEMA.md) - a full
-- should-cost `cost_record` combines material + process + tooling + overhead + SG&A +
-- margin and is M08's responsibility. This table is the material-cost-only building
-- block M08 will read from once it exists (see NEXT_AGENT.md's explicit guidance to use
-- judgment here rather than prematurely building the full cost_record shape).
CREATE TABLE material_cost_calculation (
    calculation_id         CHAR(36)      NOT NULL PRIMARY KEY,
    bom_node_id             CHAR(36)      NOT NULL,
    material_code           VARCHAR(50)   NOT NULL,
    net_weight               NUMERIC(18,6) NOT NULL,
    scrap_pct                NUMERIC(9,6)  NOT NULL,
    gross_weight             NUMERIC(18,6) NOT NULL,
    unit_rate                NUMERIC(18,6) NOT NULL,
    pricing_basis            VARCHAR(20)   NOT NULL,
    scrap_recovery_credit    NUMERIC(18,6) NOT NULL DEFAULT 0,
    material_cost            NUMERIC(18,6) NOT NULL,
    calculation_basis        VARCHAR(500),
    calculated_at             DATETIME(6)   NOT NULL,
    created_by                VARCHAR(36),
    created_at                DATETIME(6)   NOT NULL,
    updated_by                VARCHAR(36),
    updated_at                DATETIME(6),
    CONSTRAINT fk_material_cost_calc_bom_node FOREIGN KEY (bom_node_id) REFERENCES bom_node (bom_node_id)
) ENGINE = InnoDB;

-- Read pattern is always "latest calculation for this BOM node" (BomNodeService's leaf
-- resolver) or "history for this material" (MAT-07 trend reporting) - index both.
-- idempotent: idx_material_cost_calc_bom_node
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'material_cost_calculation' AND index_name = 'idx_material_cost_calc_bom_node'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_material_cost_calc_bom_node ON material_cost_calculation (bom_node_id, calculated_at)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
-- idempotent: idx_material_cost_calc_material
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'material_cost_calculation' AND index_name = 'idx_material_cost_calc_material'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_material_cost_calc_material ON material_cost_calculation (material_code, calculated_at)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- SRS 4.3.7: M03's "User Roles Involved" list includes Commodity/Material Buyer, which
-- was not among the 10 roles seeded in V1 (that seed pulled forward the roles obviously
-- needed by M01/M02; M03 needs its own). Costing Engineer and Costing Manager already exist.
INSERT INTO role (role_id, role_code, description, is_system_role)
VALUES (UUID(), 'COMMODITY_MATERIAL_BUYER', 'Manages material sourcing/pricing, material cost approval input', TRUE);
