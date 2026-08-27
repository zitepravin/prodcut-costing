-- M09 RFQ Management + M10 Supplier Costing
-- Seams already present:
--   - confidentiality_wall.rfq_event_reference (SEC-04)
--   - BomNode MAKE/BUY → BUY unit cost from supplier quote
--   - ShouldCost varianceAgainstQuote / lock negotiation seam
--   - ToolingRecord.supplier_id / tooling_quote_reference

CREATE TABLE supplier_master (
    supplier_id        CHAR(36)      NOT NULL PRIMARY KEY,
    supplier_code      VARCHAR(50)   NOT NULL,
    supplier_name      VARCHAR(200)  NOT NULL,
    country_code       VARCHAR(3),
    currency_code      VARCHAR(3)    NOT NULL DEFAULT 'USD',
    status             VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    notes              VARCHAR(500),
    created_by         VARCHAR(36),
    created_at         DATETIME(6)   NOT NULL,
    updated_by         VARCHAR(36),
    updated_at         DATETIME(6),
    CONSTRAINT uk_supplier_code UNIQUE (supplier_code),
    CONSTRAINT chk_supplier_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'BLOCKED'))
) ENGINE = InnoDB;

-- idempotent: idx_supplier_status
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'supplier_master' AND index_name = 'idx_supplier_status'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_supplier_status ON supplier_master (status)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE rfq_event (
    rfq_id             CHAR(36)      NOT NULL PRIMARY KEY,
    program_id         CHAR(36)      NOT NULL,
    rfq_number         VARCHAR(50)   NOT NULL,
    title              VARCHAR(200)  NOT NULL,
    status             VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    due_date           DATE,
    currency_code      VARCHAR(3)    NOT NULL DEFAULT 'USD',
    notes              VARCHAR(1000),
    created_by         VARCHAR(36),
    created_at         DATETIME(6)   NOT NULL,
    updated_by         VARCHAR(36),
    updated_at         DATETIME(6),
    CONSTRAINT uk_rfq_number UNIQUE (rfq_number),
    CONSTRAINT fk_rfq_program FOREIGN KEY (program_id) REFERENCES vehicle_program (program_id),
    CONSTRAINT chk_rfq_status CHECK (status IN ('DRAFT', 'OPEN', 'CLOSED', 'CANCELLED', 'AWARDED'))
) ENGINE = InnoDB;

-- idempotent: idx_rfq_program
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'rfq_event' AND index_name = 'idx_rfq_program'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_rfq_program ON rfq_event (program_id)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
-- idempotent: idx_rfq_status
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'rfq_event' AND index_name = 'idx_rfq_status'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_rfq_status ON rfq_event (status)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE rfq_line (
    rfq_line_id        CHAR(36)      NOT NULL PRIMARY KEY,
    rfq_id             CHAR(36)      NOT NULL,
    bom_node_id        CHAR(36)      NOT NULL,
    line_number        INT           NOT NULL,
    quantity           NUMERIC(18,6) NOT NULL DEFAULT 1,
    uom                VARCHAR(20),
    target_unit_price  NUMERIC(18,6),
    notes              VARCHAR(500),
    created_by         VARCHAR(36),
    created_at         DATETIME(6)   NOT NULL,
    updated_by         VARCHAR(36),
    updated_at         DATETIME(6),
    CONSTRAINT fk_rfq_line_rfq FOREIGN KEY (rfq_id) REFERENCES rfq_event (rfq_id),
    CONSTRAINT fk_rfq_line_bom FOREIGN KEY (bom_node_id) REFERENCES bom_node (bom_node_id),
    CONSTRAINT uk_rfq_line_number UNIQUE (rfq_id, line_number),
    CONSTRAINT chk_rfq_line_qty CHECK (quantity > 0)
) ENGINE = InnoDB;

-- idempotent: idx_rfq_line_bom
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'rfq_line' AND index_name = 'idx_rfq_line_bom'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_rfq_line_bom ON rfq_line (bom_node_id)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
-- idempotent: idx_rfq_line_rfq
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'rfq_line' AND index_name = 'idx_rfq_line_rfq'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_rfq_line_rfq ON rfq_line (rfq_id)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE supplier_quote (
    quote_id               CHAR(36)      NOT NULL PRIMARY KEY,
    rfq_id                 CHAR(36)      NOT NULL,
    rfq_line_id            CHAR(36)      NOT NULL,
    supplier_id            CHAR(36)      NOT NULL,
    bom_node_id            CHAR(36)      NOT NULL,
    unit_price             NUMERIC(18,6) NOT NULL,
    currency_code          VARCHAR(3)    NOT NULL DEFAULT 'USD',
    tooling_lump_sum       NUMERIC(18,6),
    lead_time_days         INT,
    valid_from             DATE,
    valid_to               DATE,
    status                 VARCHAR(20)   NOT NULL DEFAULT 'SUBMITTED',
    negotiation_reference  VARCHAR(100),
    notes                  VARCHAR(1000),
    created_by             VARCHAR(36),
    created_at             DATETIME(6)   NOT NULL,
    updated_by             VARCHAR(36),
    updated_at             DATETIME(6),
    CONSTRAINT fk_sq_rfq FOREIGN KEY (rfq_id) REFERENCES rfq_event (rfq_id),
    CONSTRAINT fk_sq_line FOREIGN KEY (rfq_line_id) REFERENCES rfq_line (rfq_line_id),
    CONSTRAINT fk_sq_supplier FOREIGN KEY (supplier_id) REFERENCES supplier_master (supplier_id),
    CONSTRAINT fk_sq_bom FOREIGN KEY (bom_node_id) REFERENCES bom_node (bom_node_id),
    CONSTRAINT chk_sq_status CHECK (status IN ('SUBMITTED', 'UNDER_REVIEW', 'ACCEPTED', 'REJECTED', 'SUPERSEDED', 'WITHDRAWN')),
    CONSTRAINT chk_sq_unit_price CHECK (unit_price >= 0)
) ENGINE = InnoDB;

-- idempotent: idx_sq_rfq
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'supplier_quote' AND index_name = 'idx_sq_rfq'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_sq_rfq ON supplier_quote (rfq_id)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
-- idempotent: idx_sq_bom_status
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'supplier_quote' AND index_name = 'idx_sq_bom_status'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_sq_bom_status ON supplier_quote (bom_node_id, status)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
-- idempotent: idx_sq_supplier
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'supplier_quote' AND index_name = 'idx_sq_supplier'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_sq_supplier ON supplier_quote (supplier_id)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
-- idempotent: idx_sq_line
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'supplier_quote' AND index_name = 'idx_sq_line'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_sq_line ON supplier_quote (rfq_line_id)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
