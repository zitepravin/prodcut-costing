-- Target Costing (target vs should-cost variance + workflow integration)
-- VehicleProgram already has program-level target_cost / target_currency (V2).
-- This module adds part-level target cost records and persisted variance snapshots.

CREATE TABLE target_cost_record (
    target_cost_id               CHAR(36)      NOT NULL PRIMARY KEY,
    bom_node_id                    CHAR(36)      NOT NULL,
    program_id                       CHAR(36)      NOT NULL,
    target_amount                      NUMERIC(18,6) NOT NULL,
    currency_code                        CHAR(3)       NOT NULL,
    effective_from                         DATE          NOT NULL,
    effective_to                           DATE,
    status                                 VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    approval_status                        VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    notes                                  VARCHAR(500),
    created_by                             VARCHAR(36),
    created_at                             DATETIME(6)   NOT NULL,
    updated_by                             VARCHAR(36),
    updated_at                             DATETIME(6),
    CONSTRAINT fk_target_cost_bom_node FOREIGN KEY (bom_node_id) REFERENCES bom_node (bom_node_id),
    CONSTRAINT fk_target_cost_program FOREIGN KEY (program_id) REFERENCES vehicle_program (program_id),
    CONSTRAINT fk_target_cost_currency FOREIGN KEY (currency_code) REFERENCES currency (currency_code),
    CONSTRAINT chk_target_cost_status CHECK (status IN ('DRAFT', 'ACTIVE', 'SUPERSEDED')),
    CONSTRAINT chk_target_cost_approval CHECK (approval_status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED')),
    CONSTRAINT chk_target_amount_positive CHECK (target_amount > 0)
) ENGINE = InnoDB;

-- idempotent: idx_target_cost_bom_node
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'target_cost_record' AND index_name = 'idx_target_cost_bom_node'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_target_cost_bom_node ON target_cost_record (bom_node_id, status)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
-- idempotent: idx_target_cost_program
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'target_cost_record' AND index_name = 'idx_target_cost_program'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_target_cost_program ON target_cost_record (program_id, status)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Append-only variance snapshots (target vs should-cost or rolled-up cost).
CREATE TABLE target_cost_variance (
    variance_id                  CHAR(36)      NOT NULL PRIMARY KEY,
    target_cost_id                 CHAR(36)      NOT NULL,
    bom_node_id                      CHAR(36)      NOT NULL,
    program_id                         CHAR(36)      NOT NULL,
    target_amount                        NUMERIC(18,6) NOT NULL,
    actual_amount                          NUMERIC(18,6) NOT NULL,
    actual_source                            VARCHAR(30)   NOT NULL,
    actual_source_id                           CHAR(36),
    absolute_variance                            NUMERIC(18,6) NOT NULL,
    percentage_variance                            NUMERIC(18,6) NOT NULL,
    exceeds_threshold                              BOOLEAN       NOT NULL DEFAULT FALSE,
    threshold_pct                                  NUMERIC(9,6),
    calculation_basis                              VARCHAR(1000),
    calculated_at                                  DATETIME(6)   NOT NULL,
    created_by                                     VARCHAR(36),
    created_at                                     DATETIME(6)   NOT NULL,
    updated_by                                     VARCHAR(36),
    updated_at                                     DATETIME(6),
    CONSTRAINT fk_variance_target FOREIGN KEY (target_cost_id) REFERENCES target_cost_record (target_cost_id),
    CONSTRAINT fk_variance_bom_node FOREIGN KEY (bom_node_id) REFERENCES bom_node (bom_node_id),
    CONSTRAINT fk_variance_program FOREIGN KEY (program_id) REFERENCES vehicle_program (program_id),
    CONSTRAINT chk_variance_source CHECK (actual_source IN ('SHOULD_COST', 'BOM_ROLLUP'))
) ENGINE = InnoDB;

-- idempotent: idx_target_variance_bom
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'target_cost_variance' AND index_name = 'idx_target_variance_bom'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_target_variance_bom ON target_cost_variance (bom_node_id, calculated_at)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
-- idempotent: idx_target_variance_target
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'target_cost_variance' AND index_name = 'idx_target_variance_target'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_target_variance_target ON target_cost_variance (target_cost_id, calculated_at)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
-- idempotent: idx_target_variance_exceeds
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'target_cost_variance' AND index_name = 'idx_target_variance_exceeds'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_target_variance_exceeds ON target_cost_variance (exceeds_threshold, calculated_at)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
