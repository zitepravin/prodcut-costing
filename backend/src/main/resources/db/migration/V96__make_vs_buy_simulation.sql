-- P2-04 Make-vs-Buy simulation (market-aligned decision support)
CREATE TABLE make_vs_buy_simulation (
    simulation_id           CHAR(36)       NOT NULL,
    program_id              CHAR(36)       NOT NULL,
    bom_node_id             CHAR(36)       NOT NULL,
    label                   VARCHAR(120)   NOT NULL,
    status                  VARCHAR(20)    NOT NULL DEFAULT 'DRAFT',
    selected_build_up_id    CHAR(36)       NULL,
    mixed_cost_id           CHAR(36)       NULL,
    recommendation_code     VARCHAR(20)    NULL,
    recommendation_summary  VARCHAR(2000)  NULL,
    decision_code           VARCHAR(20)    NULL,
    decision_reason         VARCHAR(1000)  NULL,
    decided_by              VARCHAR(36)    NULL,
    decided_at              TIMESTAMP(6)   NULL,
    bom_flag_applied        TINYINT(1)     NOT NULL DEFAULT 0,
    bom_flag_applied_at     TIMESTAMP(6)   NULL,
    frozen_at               TIMESTAMP(6)   NULL,
    reporting_currency      CHAR(3)        NULL,
    notes                   VARCHAR(1000)  NULL,
    created_by              VARCHAR(36)    NULL,
    created_at              TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by              VARCHAR(36)    NULL,
    updated_at              TIMESTAMP(6)   NULL,
    PRIMARY KEY (simulation_id),
    KEY idx_mvb_bom (bom_node_id),
    KEY idx_mvb_program_status (program_id, status)
);

CREATE TABLE make_vs_buy_leg (
    simulation_id           CHAR(36)       NOT NULL,
    leg_seq                 INT            NOT NULL,
    strategy_type           VARCHAR(20)    NOT NULL,
    source_ref_id           CHAR(36)       NOT NULL,
    supplier_id             CHAR(36)       NULL,
    unit_cost_snapshot      DECIMAL(18, 6) NULL,
    currency_snapshot       CHAR(3)        NULL,
    tco_unit_snapshot       DECIMAL(18, 6) NULL,
    tco_status              VARCHAR(20)    NOT NULL DEFAULT 'NOT_AVAILABLE',
    eligibility_status      VARCHAR(20)    NOT NULL DEFAULT 'UNKNOWN',
    eligibility_notes       VARCHAR(500)   NULL,
    risk_summary            VARCHAR(500)   NULL,
    quality_score_snapshot  DECIMAL(10, 4) NULL,
    delivery_score_snapshot DECIMAL(10, 4) NULL,
    data_status             VARCHAR(20)    NOT NULL DEFAULT 'NOT_AVAILABLE',
    PRIMARY KEY (simulation_id, leg_seq),
    CONSTRAINT fk_mvb_leg_sim FOREIGN KEY (simulation_id)
        REFERENCES make_vs_buy_simulation (simulation_id)
);
