-- M08 Should-Cost Modeling & Cost Estimation Engine (SRS 4.8, TDD 5.08, Test Plan 7.8). SC-01..SC-07.

-- SRS 4.8.5: SG&A and profit margin benchmarks configurable by commodity family and
-- region. Deliberately simple (active flag, not a full MDM-02 version chain) - see
-- SgaMarginBenchmark's Javadoc.
CREATE TABLE sga_margin_benchmark (
    benchmark_id       CHAR(36)      NOT NULL PRIMARY KEY,
    commodity_family     VARCHAR(100)  NOT NULL,
    region                  VARCHAR(50)   NOT NULL,
    sga_pct                   NUMERIC(9,6)  NOT NULL,
    margin_pct                  NUMERIC(9,6)  NOT NULL,
    active                         BOOLEAN       NOT NULL DEFAULT TRUE,
    created_by                       VARCHAR(36),
    created_at                         DATETIME(6)   NOT NULL,
    updated_by                           VARCHAR(36),
    updated_at                             DATETIME(6)
) ENGINE = InnoDB;

-- idempotent: idx_sga_margin_lookup
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'sga_margin_benchmark' AND index_name = 'idx_sga_margin_lookup'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_sga_margin_lookup ON sga_margin_benchmark (commodity_family, region, active)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- SC-02: rapid parametric $/kg benchmarks by material family x process family x region.
CREATE TABLE parametric_cost_benchmark (
    benchmark_id       CHAR(36)      NOT NULL PRIMARY KEY,
    material_family      VARCHAR(100)  NOT NULL,
    process_family          VARCHAR(100)  NOT NULL,
    region                     VARCHAR(50)   NOT NULL,
    cost_per_kg                  NUMERIC(18,6) NOT NULL,
    active                          BOOLEAN       NOT NULL DEFAULT TRUE,
    created_by                        VARCHAR(36),
    created_at                          DATETIME(6)   NOT NULL,
    updated_by                            VARCHAR(36),
    updated_at                              DATETIME(6)
) ENGINE = InnoDB;

-- idempotent: idx_parametric_benchmark_lookup
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'parametric_cost_benchmark' AND index_name = 'idx_parametric_benchmark_lookup'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_parametric_benchmark_lookup ON parametric_cost_benchmark (material_family, process_family, region, active)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- SC-01/03/06: the persisted, itemized should-cost build-up - see ShouldCostBuildUp's
-- Javadoc for why scenario_label makes this table's read pattern different from every
-- other calculation table (M03/M04/M06): multiple scenarios coexist per bom_node_id by
-- design, "latest wins" is NOT the right default read here.
CREATE TABLE should_cost_build_up (
    build_up_id              CHAR(36)      NOT NULL PRIMARY KEY,
    bom_node_id                CHAR(36)      NOT NULL,
    scenario_label                VARCHAR(100)  NOT NULL,
    material_cost                   NUMERIC(18,6) NOT NULL,
    process_cost                      NUMERIC(18,6) NOT NULL,
    tooling_cost                        NUMERIC(18,6) NOT NULL,
    packaging_freight_cost                NUMERIC(18,6) NOT NULL DEFAULT 0,
    overhead_cost                           NUMERIC(18,6) NOT NULL DEFAULT 0,
    sga_cost                                  NUMERIC(18,6) NOT NULL,
    profit_margin                               NUMERIC(18,6) NOT NULL,
    total_cost                                    NUMERIC(18,6) NOT NULL,
    stale_inputs_flag                               BOOLEAN       NOT NULL DEFAULT FALSE,
    locked                                            BOOLEAN       NOT NULL DEFAULT FALSE,
    calculation_basis                                   VARCHAR(2000),
    calculated_at                                         DATETIME(6)   NOT NULL,
    created_by                                              VARCHAR(36),
    created_at                                                DATETIME(6)   NOT NULL,
    updated_by                                                  VARCHAR(36),
    updated_at                                                    DATETIME(6),
    CONSTRAINT fk_should_cost_build_up_bom_node FOREIGN KEY (bom_node_id) REFERENCES bom_node (bom_node_id)
) ENGINE = InnoDB;

-- idempotent: idx_should_cost_build_up_bom_node
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'should_cost_build_up' AND index_name = 'idx_should_cost_build_up_bom_node'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_should_cost_build_up_bom_node ON should_cost_build_up (bom_node_id, calculated_at)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
-- idempotent: idx_should_cost_build_up_scenario
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'should_cost_build_up' AND index_name = 'idx_should_cost_build_up_scenario'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_should_cost_build_up_scenario ON should_cost_build_up (bom_node_id, scenario_label, calculated_at)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- SRS 4.8.7: M08's "User Roles Involved" includes Product Cost Engineer, not among the
-- roles seeded so far. "Purchasing/Sourcing Manager" is mapped to the existing
-- SOURCING_LEAD role at the application layer (see ShouldCostController's Javadoc)
-- rather than adding a near-duplicate role - flagged for client confirmation alongside
-- this project's other role-mapping decisions.
INSERT INTO role (role_id, role_code, description, is_system_role)
VALUES (UUID(), 'PRODUCT_COST_ENGINEER', 'Builds and reviews should-cost estimates, challenges supplier quotes', TRUE);
