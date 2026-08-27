-- V2: Master Data Management module (M01) - DATABASE_SCHEMA.md Section 2.
-- Reference tables first (no FK dependents within this migration), then the four
-- master-rate tables that share the AbstractMasterRateRecord column shape.

CREATE TABLE currency (
    currency_code    CHAR(3)     NOT NULL PRIMARY KEY,
    description      VARCHAR(50),
    decimal_places   TINYINT
) ENGINE = InnoDB;

CREATE TABLE uom_master (
    uom_code      VARCHAR(10)  NOT NULL PRIMARY KEY,
    description   VARCHAR(50)
) ENGINE = InnoDB;

CREATE TABLE plant_master (
    plant_id               CHAR(36)     NOT NULL PRIMARY KEY,
    plant_code              VARCHAR(20)  NOT NULL,
    country                  CHAR(2),
    statutory_duty_notes     TEXT,
    freight_zone             VARCHAR(50),
    CONSTRAINT uk_plant_code UNIQUE (plant_code)
) ENGINE = InnoDB;

CREATE TABLE process_master (
    process_id                   CHAR(36)     NOT NULL PRIMARY KEY,
    process_name                 VARCHAR(100) NOT NULL,
    default_cycle_time_formula   TEXT,
    default_scrap_pct            NUMERIC(9,6),
    CONSTRAINT uk_process_name UNIQUE (process_name)
) ENGINE = InnoDB;

CREATE TABLE vehicle_program (
    program_id         CHAR(36)      NOT NULL PRIMARY KEY,
    program_code       VARCHAR(50)   NOT NULL,
    model_name         VARCHAR(100),
    platform_code      VARCHAR(50),
    sourcing_region    VARCHAR(50),
    lifetime_volume    NUMERIC(18,2),
    target_cost        NUMERIC(18,6),
    target_currency    CHAR(3),
    status             VARCHAR(20)   NOT NULL DEFAULT 'Active',
    created_by         VARCHAR(36),
    created_at         DATETIME(6)   NOT NULL,
    updated_by         VARCHAR(36),
    updated_at         DATETIME(6),
    CONSTRAINT uk_program_code UNIQUE (program_code),
    CONSTRAINT fk_program_currency FOREIGN KEY (target_currency) REFERENCES currency (currency_code)
) ENGINE = InnoDB;

-- material_master, machine_master, labor_master, overhead_pool share the
-- AbstractMasterRateRecord shape (record_id, code, description, rate, uom, plant_id,
-- effective_from/to, version_number, status, audit columns) plus type-specific columns.

CREATE TABLE material_master (
    record_id                CHAR(36)      NOT NULL PRIMARY KEY,
    code                      VARCHAR(50)   NOT NULL,
    description               VARCHAR(255),
    rate                      NUMERIC(18,6) NOT NULL,
    uom                       VARCHAR(20),
    plant_id                  CHAR(36),
    effective_from            DATE          NOT NULL,
    effective_to              DATE,
    version_number            INT           NOT NULL,
    status                    VARCHAR(30)   NOT NULL,
    grade                     VARCHAR(50),
    specification_standard    VARCHAR(50),
    density                   NUMERIC(18,6),
    scrap_recovery_rate       NUMERIC(18,6),
    material_form             VARCHAR(30),
    created_by                VARCHAR(36),
    created_at                DATETIME(6)   NOT NULL,
    updated_by                VARCHAR(36),
    updated_at                DATETIME(6),
    CONSTRAINT fk_material_plant FOREIGN KEY (plant_id) REFERENCES plant_master (plant_id)
) ENGINE = InnoDB;

-- MDM-01: code is unique per active/pending lineage head. We do not put a hard UNIQUE
-- constraint on `code` alone (multiple *versions* of the same code are expected rows),
-- so uniqueness of the "current" code is enforced at the service layer (existsByCode
-- checks across all versions before allowing a brand-new code to be created) plus this
-- composite index for fast lookup of a code's version history.
CREATE UNIQUE INDEX uk_material_code_version ON material_master (code, version_number);
-- idempotent: idx_material_status
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'material_master' AND index_name = 'idx_material_status'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_material_status ON material_master (status)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
-- idempotent: idx_material_plant
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'material_master' AND index_name = 'idx_material_plant'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_material_plant ON material_master (plant_id)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE machine_master (
    record_id                     CHAR(36)      NOT NULL PRIMARY KEY,
    code                           VARCHAR(50)   NOT NULL,
    description                    VARCHAR(255),
    rate                           NUMERIC(18,6) NOT NULL,
    uom                            VARCHAR(20),
    plant_id                       CHAR(36),
    effective_from                 DATE          NOT NULL,
    effective_to                   DATE,
    version_number                 INT           NOT NULL,
    status                         VARCHAR(30)   NOT NULL,
    machine_type                   VARCHAR(100),
    capacity                       VARCHAR(100),
    depreciation_life_years        NUMERIC(9,2),
    capital_cost                   NUMERIC(18,6),
    annual_operating_hours         NUMERIC(18,2),
    power_cost_per_hour             NUMERIC(18,6),
    maintenance_cost_per_hour       NUMERIC(18,6),
    floor_space_cost_per_hour       NUMERIC(18,6),
    shift_pattern                  VARCHAR(20),
    created_by                     VARCHAR(36),
    created_at                     DATETIME(6)   NOT NULL,
    updated_by                     VARCHAR(36),
    updated_at                     DATETIME(6),
    CONSTRAINT fk_machine_plant FOREIGN KEY (plant_id) REFERENCES plant_master (plant_id)
) ENGINE = InnoDB;

CREATE UNIQUE INDEX uk_machine_code_version ON machine_master (code, version_number);
-- idempotent: idx_machine_status
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'machine_master' AND index_name = 'idx_machine_status'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_machine_status ON machine_master (status)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE labor_master (
    record_id             CHAR(36)      NOT NULL PRIMARY KEY,
    code                   VARCHAR(50)   NOT NULL,
    description            VARCHAR(255),
    rate                   NUMERIC(18,6) NOT NULL,
    uom                    VARCHAR(20),
    plant_id               CHAR(36),
    effective_from         DATE          NOT NULL,
    effective_to           DATE,
    version_number         INT           NOT NULL,
    status                 VARCHAR(30)   NOT NULL,
    operator_grade         VARCHAR(50),
    shift_type             VARCHAR(20),
    country_region         VARCHAR(50),
    statutory_burden_pct   NUMERIC(9,6),
    created_by             VARCHAR(36),
    created_at             DATETIME(6)   NOT NULL,
    updated_by             VARCHAR(36),
    updated_at             DATETIME(6),
    CONSTRAINT fk_labor_plant FOREIGN KEY (plant_id) REFERENCES plant_master (plant_id)
) ENGINE = InnoDB;

CREATE UNIQUE INDEX uk_labor_code_version ON labor_master (code, version_number);

CREATE TABLE overhead_pool (
    record_id         CHAR(36)      NOT NULL PRIMARY KEY,
    code               VARCHAR(50)   NOT NULL,
    description        VARCHAR(255),
    rate               NUMERIC(18,6) NOT NULL,
    uom                VARCHAR(20),
    plant_id           CHAR(36),
    effective_from     DATE          NOT NULL,
    effective_to       DATE,
    version_number     INT           NOT NULL,
    status             VARCHAR(30)   NOT NULL,
    pool_category      VARCHAR(50),
    allocation_base    VARCHAR(100),
    period             VARCHAR(20),
    created_by         VARCHAR(36),
    created_at         DATETIME(6)   NOT NULL,
    updated_by         VARCHAR(36),
    updated_at         DATETIME(6),
    CONSTRAINT fk_overhead_plant FOREIGN KEY (plant_id) REFERENCES plant_master (plant_id)
) ENGINE = InnoDB;

CREATE UNIQUE INDEX uk_overhead_code_version ON overhead_pool (code, version_number);

-- Seed reference data needed for Sprint 1 smoke testing / Test Plan Section 5.2
-- ("at least three currencies configured with test exchange rates").
INSERT INTO currency (currency_code, description, decimal_places) VALUES
    ('USD', 'US Dollar', 2),
    ('EUR', 'Euro', 2),
    ('JPY', 'Japanese Yen', 0),
    ('INR', 'Indian Rupee', 2);

INSERT INTO uom_master (uom_code, description) VALUES
    ('KG', 'Kilogram'),
    ('EA', 'Each'),
    ('HR', 'Hour'),
    ('MM', 'Millimeter'),
    ('L', 'Liter');
