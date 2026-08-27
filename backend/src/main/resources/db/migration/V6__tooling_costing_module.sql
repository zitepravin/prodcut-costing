-- M06 Tooling, Die & Fixture Investment Costing (SRS 4.6, TDD 5.06, Test Plan 7.6). TL-01..TL-07.

CREATE TABLE tooling_record (
    tooling_record_id            CHAR(36)      NOT NULL PRIMARY KEY,
    bom_node_id                    CHAR(36)      NOT NULL,
    tooling_type                     VARCHAR(30)   NOT NULL,
    description                        VARCHAR(500),
    cavity_count                         INT,
    tool_cost                              NUMERIC(18,6) NOT NULL,
    tool_life_hits                           NUMERIC(18,2),
    refurbishment_cost_per_event               NUMERIC(18,6),
    cumulative_volume_to_date                    NUMERIC(18,2),
    ownership                                      VARCHAR(20)   NOT NULL,
    supplier_id                                      VARCHAR(50),
    tooling_quote_reference                            VARCHAR(100),
    plant_location                                       VARCHAR(100),
    installation_date                                      DATE,
    asset_status                                             VARCHAR(20)   NOT NULL,
    created_by                                                 VARCHAR(36),
    created_at                                                   DATETIME(6)   NOT NULL,
    updated_by                                                     VARCHAR(36),
    updated_at                                                       DATETIME(6),
    CONSTRAINT fk_tooling_record_bom_node FOREIGN KEY (bom_node_id) REFERENCES bom_node (bom_node_id)
) ENGINE = InnoDB;

-- TL-06: more than one tooling record per BOM node is expected (comparing supplier
-- proposals for the same part's tooling), so bom_node_id is indexed but NOT unique -
-- same rationale as process_routing.bom_node_id (M04) and material's one-node-many-
-- calculations pattern (M03).
-- idempotent: idx_tooling_record_bom_node
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'tooling_record' AND index_name = 'idx_tooling_record_bom_node'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_tooling_record_bom_node ON tooling_record (bom_node_id)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
-- idempotent: idx_tooling_record_plant_status
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'tooling_record' AND index_name = 'idx_tooling_record_plant_status'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_tooling_record_plant_status ON tooling_record (plant_location, asset_status)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- TL-02/TL-04: persisted amortization result. Mirrors material_cost_calculation /
-- process_cost_calculation's shape and rationale exactly - append-only, program_id
-- denormalized (not just bom_node_id) since TL-02's formula needs the owning program's
-- lifetime_volume and that's worth keeping visible on the calculation row itself for
-- audit/traceability without a join back through bom_node -> vehicle_program every time.
CREATE TABLE tooling_cost_calculation (
    calculation_id                CHAR(36)      NOT NULL PRIMARY KEY,
    tooling_record_id               CHAR(36)      NOT NULL,
    bom_node_id                       CHAR(36)      NOT NULL,
    program_id                          CHAR(36)      NOT NULL,
    base_amortized_cost_per_piece         NUMERIC(18,6) NOT NULL,
    refurbishment_events_projected           INT,
    refurbishment_cost_per_piece               NUMERIC(18,6),
    total_amortized_cost_per_piece               NUMERIC(18,6) NOT NULL,
    included_in_piece_price                        BOOLEAN       NOT NULL,
    calculation_basis                                VARCHAR(1000),
    calculated_at                                      DATETIME(6)   NOT NULL,
    created_by                                           VARCHAR(36),
    created_at                                             DATETIME(6)   NOT NULL,
    updated_by                                               VARCHAR(36),
    updated_at                                                 DATETIME(6),
    CONSTRAINT fk_tooling_cost_calc_record FOREIGN KEY (tooling_record_id) REFERENCES tooling_record (tooling_record_id),
    CONSTRAINT fk_tooling_cost_calc_bom_node FOREIGN KEY (bom_node_id) REFERENCES bom_node (bom_node_id),
    CONSTRAINT fk_tooling_cost_calc_program FOREIGN KEY (program_id) REFERENCES vehicle_program (program_id)
) ENGINE = InnoDB;

-- idempotent: idx_tooling_cost_calc_bom_node
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'tooling_cost_calculation' AND index_name = 'idx_tooling_cost_calc_bom_node'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_tooling_cost_calc_bom_node ON tooling_cost_calculation (bom_node_id, calculated_at)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
-- idempotent: idx_tooling_cost_calc_record
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'tooling_cost_calculation' AND index_name = 'idx_tooling_cost_calc_record'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_tooling_cost_calc_record ON tooling_cost_calculation (tooling_record_id, calculated_at)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- TL-03: "multi-cavity/multi-impression tooling with cost and cycle-time impact
-- reflected in both tooling and process cost." The tooling-side impact is tooling_record
-- .cavity_count (already present); this column is the process-side half of the same
-- requirement - see ProcessOperation's and ProcessCostEngine's Javadoc for the full
-- rationale. Nullable, defaults to 1 (single-cavity) at the application layer so this is
-- fully backward compatible with routings created before M06 existed.
ALTER TABLE process_operation
    ADD COLUMN output_parts_per_cycle NUMERIC(9,2);

-- No new role needed - SRS 4.6.7's "Tooling Engineer" (TOOLING_ENGINEER) was already
-- seeded in V1 alongside Costing Engineer and Purchasing (PURCHASING_BUYER).
