-- M07 Packaging & Logistics Costing
-- Fills the packaging_freight_cost element of the SRS Appendix D nine-element should-cost.
-- Plant.freight_zone is the existing master-data hook for zone-based freight rates.

CREATE TABLE packaging_logistics_profile (
    profile_id                     CHAR(36)      NOT NULL PRIMARY KEY,
    bom_node_id                      CHAR(36)      NOT NULL,
    profile_label                      VARCHAR(100)  NOT NULL DEFAULT 'Standard',
    packaging_type                       VARCHAR(40)   NOT NULL,
    primary_pack_cost_per_piece            NUMERIC(18,6) NOT NULL DEFAULT 0,
    secondary_pack_cost_per_piece            NUMERIC(18,6) NOT NULL DEFAULT 0,
    returnable_container_cost                NUMERIC(18,6),
    container_life_cycles                      NUMERIC(18,2),
    net_weight_kg                              NUMERIC(18,6),
    freight_mode                               VARCHAR(20)   NOT NULL DEFAULT 'OUTBOUND',
    freight_rate_per_kg                        NUMERIC(18,6),
    fixed_freight_per_unit                     NUMERIC(18,6),
    duty_pct                                   NUMERIC(9,6),
    origin_plant_id                            CHAR(36),
    destination_freight_zone                   VARCHAR(50),
    notes                                      VARCHAR(500),
    active                                     BOOLEAN       NOT NULL DEFAULT TRUE,
    created_by                                 VARCHAR(36),
    created_at                                 DATETIME(6)   NOT NULL,
    updated_by                                 VARCHAR(36),
    updated_at                                 DATETIME(6),
    CONSTRAINT fk_pkg_profile_bom_node FOREIGN KEY (bom_node_id) REFERENCES bom_node (bom_node_id),
    CONSTRAINT fk_pkg_profile_plant FOREIGN KEY (origin_plant_id) REFERENCES plant_master (plant_id),
    CONSTRAINT chk_pkg_freight_mode CHECK (freight_mode IN ('NONE', 'INBOUND', 'OUTBOUND', 'BOTH')),
    CONSTRAINT chk_pkg_duty_pct CHECK (duty_pct IS NULL OR (duty_pct >= 0 AND duty_pct < 1))
) ENGINE = InnoDB;

-- idempotent: idx_pkg_profile_bom_node
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'packaging_logistics_profile' AND index_name = 'idx_pkg_profile_bom_node'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_pkg_profile_bom_node ON packaging_logistics_profile (bom_node_id)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
-- idempotent: idx_pkg_profile_active
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'packaging_logistics_profile' AND index_name = 'idx_pkg_profile_active'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_pkg_profile_active ON packaging_logistics_profile (bom_node_id, active)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Append-only calculation results (same pattern as material/process/tooling cost calculations).
CREATE TABLE packaging_logistics_cost_calculation (
    calculation_id                 CHAR(36)      NOT NULL PRIMARY KEY,
    profile_id                       CHAR(36)      NOT NULL,
    bom_node_id                        CHAR(36)      NOT NULL,
    program_id                           CHAR(36)      NOT NULL,
    packaging_cost_per_piece               NUMERIC(18,6) NOT NULL,
    freight_cost_per_piece                   NUMERIC(18,6) NOT NULL,
    duty_cost_per_piece                        NUMERIC(18,6) NOT NULL,
    total_packaging_freight_cost                 NUMERIC(18,6) NOT NULL,
    calculation_basis                              VARCHAR(2000),
    calculated_at                                    DATETIME(6)   NOT NULL,
    created_by                                         VARCHAR(36),
    created_at                                           DATETIME(6)   NOT NULL,
    updated_by                                             VARCHAR(36),
    updated_at                                               DATETIME(6),
    CONSTRAINT fk_pkg_calc_profile FOREIGN KEY (profile_id) REFERENCES packaging_logistics_profile (profile_id),
    CONSTRAINT fk_pkg_calc_bom_node FOREIGN KEY (bom_node_id) REFERENCES bom_node (bom_node_id),
    CONSTRAINT fk_pkg_calc_program FOREIGN KEY (program_id) REFERENCES vehicle_program (program_id)
) ENGINE = InnoDB;

-- idempotent: idx_pkg_calc_bom_node
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'packaging_logistics_cost_calculation' AND index_name = 'idx_pkg_calc_bom_node'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_pkg_calc_bom_node ON packaging_logistics_cost_calculation (bom_node_id, calculated_at)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
-- idempotent: idx_pkg_calc_profile
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'packaging_logistics_cost_calculation' AND index_name = 'idx_pkg_calc_profile'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_pkg_calc_profile ON packaging_logistics_cost_calculation (profile_id, calculated_at)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
