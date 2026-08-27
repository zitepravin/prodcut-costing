-- D1 Activity types + D2 Capacity / utilization model

CREATE TABLE IF NOT EXISTS activity_type (
    activity_type_id    CHAR(36)      NOT NULL PRIMARY KEY,
    activity_code       VARCHAR(40)   NOT NULL,
    activity_name       VARCHAR(200)  NOT NULL,
    category            VARCHAR(40)   NOT NULL DEFAULT 'MACHINE',
    -- MACHINE | LABOR | SETUP | ENERGY | QUALITY | OTHER
    unit_of_measure     VARCHAR(20)   NOT NULL DEFAULT 'HR',
    default_rate        DECIMAL(18,6) NULL,
    currency_code       CHAR(3)       NOT NULL DEFAULT 'USD',
    cost_center_code    VARCHAR(40)   NULL,
    active              BOOLEAN       NOT NULL DEFAULT TRUE,
    notes               VARCHAR(500)  NULL,
    created_at          TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_activity_code (activity_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS capacity_center (
    capacity_center_id  CHAR(36)      NOT NULL PRIMARY KEY,
    center_code         VARCHAR(40)   NOT NULL,
    center_name         VARCHAR(200)  NOT NULL,
    plant_code          VARCHAR(40)   NULL,
    available_hours     DECIMAL(18,6) NOT NULL,
    period_code         VARCHAR(20)   NOT NULL,
    -- e.g. 2026-Q3 or 2026-08
    efficiency_pct      DECIMAL(9,6)  NOT NULL DEFAULT 1.000000,
    -- 1.0 = 100% efficiency applied to available hours
    active              BOOLEAN       NOT NULL DEFAULT TRUE,
    notes               VARCHAR(500)  NULL,
    created_at          TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_cc_code_period (center_code, period_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS capacity_load (
    load_id             CHAR(36)      NOT NULL PRIMARY KEY,
    capacity_center_id  CHAR(36)      NOT NULL,
    activity_type_id    CHAR(36)      NULL,
    source_ref_type     VARCHAR(40)   NULL,
    source_ref_id       CHAR(36)      NULL,
    required_hours      DECIMAL(18,6) NOT NULL,
    load_label          VARCHAR(200)  NULL,
    created_at          TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_cl_center FOREIGN KEY (capacity_center_id) REFERENCES capacity_center(capacity_center_id),
    KEY idx_cl_center (capacity_center_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
