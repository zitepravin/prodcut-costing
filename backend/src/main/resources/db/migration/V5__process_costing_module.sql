-- M04 Process & Labor Costing (SRS 4.4, TDD 5.04, Test Plan 7.4). PRC-01..PRC-08.

CREATE TABLE process_routing (
    routing_id    CHAR(36)     NOT NULL PRIMARY KEY,
    bom_node_id    CHAR(36)     NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    created_by      VARCHAR(36),
    created_at      DATETIME(6)  NOT NULL,
    updated_by      VARCHAR(36),
    updated_at      DATETIME(6),
    CONSTRAINT fk_process_routing_bom_node FOREIGN KEY (bom_node_id) REFERENCES bom_node (bom_node_id)
) ENGINE = InnoDB;

-- PRC-07: more than one routing per BOM node is expected (alternate routing comparison),
-- so bom_node_id is indexed but NOT unique.
-- idempotent: idx_process_routing_bom_node
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'process_routing' AND index_name = 'idx_process_routing_bom_node'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_process_routing_bom_node ON process_routing (bom_node_id)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE process_operation (
    operation_id        CHAR(36)      NOT NULL PRIMARY KEY,
    routing_id           CHAR(36)      NOT NULL,
    sequence_number       INT           NOT NULL,
    process_name           VARCHAR(100),
    machine_code            VARCHAR(50),
    labor_code               VARCHAR(50),
    cycle_time_seconds        NUMERIC(18,6) NOT NULL,
    operator_count              NUMERIC(9,6)  NOT NULL,
    setup_time_seconds            NUMERIC(18,6),
    batch_size                      NUMERIC(18,6),
    created_by                        VARCHAR(36),
    created_at                          DATETIME(6)   NOT NULL,
    updated_by                            VARCHAR(36),
    updated_at                              DATETIME(6),
    CONSTRAINT fk_process_operation_routing FOREIGN KEY (routing_id) REFERENCES process_routing (routing_id)
) ENGINE = InnoDB;

-- idempotent: idx_process_operation_routing_seq
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'process_operation' AND index_name = 'idx_process_operation_routing_seq'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_process_operation_routing_seq ON process_operation (routing_id, sequence_number)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- PRC-06: persisted routing-level roll-up result. Mirrors material_cost_calculation's
-- shape/rationale exactly (M03) - append-only, bom_node_id denormalized for a single-query
-- "latest process cost for this node" lookup by BomRollupEngine.LeafUnitCostResolver.
CREATE TABLE process_cost_calculation (
    calculation_id      CHAR(36)      NOT NULL PRIMARY KEY,
    routing_id            CHAR(36)      NOT NULL,
    bom_node_id             CHAR(36)      NOT NULL,
    total_process_cost        NUMERIC(18,6) NOT NULL,
    calculation_basis            VARCHAR(1000),
    calculated_at                  DATETIME(6)   NOT NULL,
    created_by                       VARCHAR(36),
    created_at                         DATETIME(6)   NOT NULL,
    updated_by                           VARCHAR(36),
    updated_at                             DATETIME(6),
    CONSTRAINT fk_process_cost_calc_routing FOREIGN KEY (routing_id) REFERENCES process_routing (routing_id),
    CONSTRAINT fk_process_cost_calc_bom_node FOREIGN KEY (bom_node_id) REFERENCES bom_node (bom_node_id)
) ENGINE = InnoDB;

-- idempotent: idx_process_cost_calc_bom_node
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'process_cost_calculation' AND index_name = 'idx_process_cost_calc_bom_node'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_process_cost_calc_bom_node ON process_cost_calculation (bom_node_id, calculated_at)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
-- idempotent: idx_process_cost_calc_routing
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'process_cost_calculation' AND index_name = 'idx_process_cost_calc_routing'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_process_cost_calc_routing ON process_cost_calculation (routing_id, calculated_at)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- PRC-03: "pre-built cycle-time calculators for at least eight standard automotive
-- processes." default_cycle_time_formula is a human-readable formula reference (TDD
-- 4.4.3's "built-in cycle-time formula library") consumed by GET
-- /api/v1/costing/process/cycle-time-templates - not an executable expression, since no
-- parseable formula syntax is specified anywhere in the SRS/TDD (an engineer still enters
-- the resulting cycleTimeSeconds directly on each ProcessOperation; this table documents
-- which reference formula they should have used).
INSERT INTO process_master (process_id, process_name, default_cycle_time_formula, default_scrap_pct) VALUES
    (UUID(), 'Stamping', 'strokes-per-minute (SPM) based: cycle time = 60 / SPM per stroke', 0.120000),
    (UUID(), 'Forging', 'press cycles per part x cycle time per stroke, plus die-change amortization', 0.080000),
    (UUID(), 'Casting', 'mold fill time + cooling time + ejection time per cavity', 0.050000),
    (UUID(), 'Injection Molding', 'fill time + cooling time + ejection time, per cavitation count', 0.030000),
    (UUID(), 'Machining', 'feed/speed based: cycle time = (cut length / feed rate) x number of passes', 0.020000),
    (UUID(), 'Welding', 'weld length / travel speed, plus spot count x time-per-spot for spot welds', 0.010000),
    (UUID(), 'Painting', 'line speed based: cycle time = part surface area / coverage rate', 0.020000),
    (UUID(), 'Assembly', 'MTM / predetermined time system: sum of standard motion-time units per operation', 0.005000)
ON DUPLICATE KEY UPDATE process_name = process_name;

-- SRS 4.4.7: M04's "User Roles Involved" includes Industrial Engineer, not among the 10
-- roles V1 seeded (Manufacturing/Process Engineer -> PROCESS_ENGINEER and Costing Engineer
-- already exist from V1).
INSERT INTO role (role_id, role_code, description, is_system_role)
VALUES (UUID(), 'INDUSTRIAL_ENGINEER', 'Defines process routings, cycle times, line balancing', TRUE);
