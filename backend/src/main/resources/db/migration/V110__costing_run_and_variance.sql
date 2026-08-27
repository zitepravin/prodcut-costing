-- B1 Costing Run / Mass Costing

CREATE TABLE IF NOT EXISTS costing_run (
    run_id            CHAR(36)      NOT NULL PRIMARY KEY,
    run_code          VARCHAR(40)   NOT NULL,
    description       VARCHAR(500)  NULL,
    status            VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    program_id        CHAR(36)      NULL,
    plant_code        VARCHAR(40)   NULL,
    costing_variant_id CHAR(36)     NULL,
    costing_sheet_id  CHAR(36)      NULL,
    lot_size          DECIMAL(18,6) NULL,
    costing_date      DATE          NULL,
    success_count     INT           NOT NULL DEFAULT 0,
    failure_count     INT           NOT NULL DEFAULT 0,
    started_at        TIMESTAMP(6)  NULL,
    completed_at      TIMESTAMP(6)  NULL,
    created_by        VARCHAR(36)   NULL,
    created_at        TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by        VARCHAR(36)   NULL,
    updated_at        TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_costing_run_code (run_code),
    KEY idx_costing_run_program (program_id),
    KEY idx_costing_run_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS costing_run_item (
    item_id           CHAR(36)      NOT NULL PRIMARY KEY,
    run_id            CHAR(36)      NOT NULL,
    bom_node_id       CHAR(36)      NOT NULL,
    part_number       VARCHAR(50)   NULL,
    status            VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    unit_cost         DECIMAL(18,6) NULL,
    error_message     VARCHAR(1000) NULL,
    estimate_basis    VARCHAR(2000) NULL,
    calculated_at     TIMESTAMP(6)  NULL,
    CONSTRAINT fk_cri_run FOREIGN KEY (run_id) REFERENCES costing_run(run_id),
    KEY idx_cri_run (run_id),
    KEY idx_cri_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
