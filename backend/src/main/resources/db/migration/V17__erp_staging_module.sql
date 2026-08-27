-- V17: ERP / PLM integration staging (stubs)
-- Inbound staging only — no automatic write-through to live masters until a future promote job.
-- Aligns with TASKS.md "ERP integration stubs / staging tables" and CAD/PLM net-weight seam (MAT).

CREATE TABLE erp_inbound_batch (
    batch_id           CHAR(36)      NOT NULL PRIMARY KEY,
    source_system      VARCHAR(50)   NOT NULL,
    batch_type         VARCHAR(30)   NOT NULL,
    status             VARCHAR(20)   NOT NULL DEFAULT 'RECEIVED',
    row_count          INT           NOT NULL DEFAULT 0,
    error_count        INT           NOT NULL DEFAULT 0,
    source_file_name   VARCHAR(255),
    notes              VARCHAR(1000),
    received_at        DATETIME(6)   NOT NULL,
    processed_at       DATETIME(6),
    created_by         VARCHAR(36),
    created_at         DATETIME(6)   NOT NULL,
    updated_by         VARCHAR(36),
    updated_at         DATETIME(6),
    CONSTRAINT chk_erp_batch_type CHECK (batch_type IN ('PART', 'BOM', 'MATERIAL_PRICE', 'MIXED')),
    CONSTRAINT chk_erp_batch_status CHECK (status IN ('RECEIVED', 'VALIDATING', 'READY', 'PROMOTED', 'FAILED', 'CANCELLED'))
) ENGINE = InnoDB;

-- idempotent: idx_erp_batch_status
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'erp_inbound_batch' AND index_name = 'idx_erp_batch_status'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_erp_batch_status ON erp_inbound_batch (status)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
-- idempotent: idx_erp_batch_source
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'erp_inbound_batch' AND index_name = 'idx_erp_batch_source'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_erp_batch_source ON erp_inbound_batch (source_system, received_at)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE erp_part_staging (
    staging_id         CHAR(36)      NOT NULL PRIMARY KEY,
    batch_id           CHAR(36)      NOT NULL,
    external_part_id   VARCHAR(100)  NOT NULL,
    part_number        VARCHAR(50)   NOT NULL,
    revision           VARCHAR(20),
    description        VARCHAR(500),
    net_weight_kg      NUMERIC(18,6),
    uom                VARCHAR(20),
    material_code      VARCHAR(50),
    make_buy_code      VARCHAR(10),
    plant_code         VARCHAR(50),
    program_code       VARCHAR(50),
    status             VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    error_message      VARCHAR(1000),
    raw_payload        TEXT,
    created_at         DATETIME(6)   NOT NULL,
    updated_at         DATETIME(6),
    CONSTRAINT fk_erp_part_batch FOREIGN KEY (batch_id) REFERENCES erp_inbound_batch (batch_id),
    CONSTRAINT chk_erp_part_status CHECK (status IN ('PENDING', 'VALID', 'INVALID', 'PROMOTED', 'SKIPPED'))
) ENGINE = InnoDB;

-- idempotent: idx_erp_part_batch
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'erp_part_staging' AND index_name = 'idx_erp_part_batch'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_erp_part_batch ON erp_part_staging (batch_id)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
-- idempotent: idx_erp_part_number
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'erp_part_staging' AND index_name = 'idx_erp_part_number'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_erp_part_number ON erp_part_staging (part_number, revision)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE erp_bom_staging (
    staging_id         CHAR(36)      NOT NULL PRIMARY KEY,
    batch_id           CHAR(36)      NOT NULL,
    external_bom_id    VARCHAR(100),
    parent_part_number VARCHAR(50)   NOT NULL,
    parent_revision    VARCHAR(20),
    child_part_number  VARCHAR(50)   NOT NULL,
    child_revision     VARCHAR(20),
    quantity_per       NUMERIC(18,6) NOT NULL,
    uom                VARCHAR(20),
    line_number        INT,
    plant_code         VARCHAR(50),
    program_code       VARCHAR(50),
    status             VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    error_message      VARCHAR(1000),
    raw_payload        TEXT,
    created_at         DATETIME(6)   NOT NULL,
    updated_at         DATETIME(6),
    CONSTRAINT fk_erp_bom_batch FOREIGN KEY (batch_id) REFERENCES erp_inbound_batch (batch_id),
    CONSTRAINT chk_erp_bom_status CHECK (status IN ('PENDING', 'VALID', 'INVALID', 'PROMOTED', 'SKIPPED')),
    CONSTRAINT chk_erp_bom_qty CHECK (quantity_per > 0)
) ENGINE = InnoDB;

-- idempotent: idx_erp_bom_batch
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'erp_bom_staging' AND index_name = 'idx_erp_bom_batch'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_erp_bom_batch ON erp_bom_staging (batch_id)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
-- idempotent: idx_erp_bom_parent
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'erp_bom_staging' AND index_name = 'idx_erp_bom_parent'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_erp_bom_parent ON erp_bom_staging (parent_part_number)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE erp_material_price_staging (
    staging_id         CHAR(36)      NOT NULL PRIMARY KEY,
    batch_id           CHAR(36)      NOT NULL,
    material_code      VARCHAR(50)   NOT NULL,
    plant_code         VARCHAR(50),
    unit_price         NUMERIC(18,6) NOT NULL,
    currency_code      CHAR(3)       NOT NULL,
    uom                VARCHAR(20),
    effective_from     DATE          NOT NULL,
    effective_to       DATE,
    supplier_code      VARCHAR(50),
    status             VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    error_message      VARCHAR(1000),
    raw_payload        TEXT,
    created_at         DATETIME(6)   NOT NULL,
    updated_at         DATETIME(6),
    CONSTRAINT fk_erp_price_batch FOREIGN KEY (batch_id) REFERENCES erp_inbound_batch (batch_id),
    CONSTRAINT chk_erp_price_status CHECK (status IN ('PENDING', 'VALID', 'INVALID', 'PROMOTED', 'SKIPPED')),
    CONSTRAINT chk_erp_price_positive CHECK (unit_price >= 0)
) ENGINE = InnoDB;

-- idempotent: idx_erp_price_batch
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'erp_material_price_staging' AND index_name = 'idx_erp_price_batch'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_erp_price_batch ON erp_material_price_staging (batch_id)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
-- idempotent: idx_erp_price_material
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'erp_material_price_staging' AND index_name = 'idx_erp_price_material'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_erp_price_material ON erp_material_price_staging (material_code, effective_from)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
