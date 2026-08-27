-- P2-02: Program volume curves for lifecycle cost forecasting
CREATE TABLE program_volume_curve (
    curve_id        CHAR(36)     NOT NULL,
    program_id      CHAR(36)     NOT NULL,
    scenario_code   VARCHAR(40)  NOT NULL,
    scenario_label  VARCHAR(120) NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    notes           VARCHAR(500) NULL,
    created_by      VARCHAR(36)  NULL,
    created_at      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by      VARCHAR(36)  NULL,
    updated_at      TIMESTAMP(6) NULL,
    PRIMARY KEY (curve_id),
    KEY idx_pvc_program (program_id),
    KEY idx_pvc_program_scenario (program_id, scenario_code, status)
);

CREATE TABLE program_volume_curve_year (
    curve_id        CHAR(36)       NOT NULL,
    year_index      INT            NOT NULL,
    calendar_year   INT            NULL,
    planned_volume  DECIMAL(18, 2) NOT NULL,
    PRIMARY KEY (curve_id, year_index),
    CONSTRAINT fk_pvcy_curve FOREIGN KEY (curve_id) REFERENCES program_volume_curve (curve_id)
);
