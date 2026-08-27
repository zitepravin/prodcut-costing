-- V45: Questionnaire evaluation aggregate snapshot (mandatory gates + weighted score)
-- Historical answers remain bound to question versions; this stores the evaluation outcome.
CREATE TABLE vendor_questionnaire_evaluation (
    evaluation_id CHAR(36)  NOT NULL,
    supplier_id CHAR(36)  NOT NULL,
    overall_eligible        BOOLEAN      NOT NULL,
    mandatory_gates_passed  BOOLEAN      NOT NULL,
    required_answered       BOOLEAN      NOT NULL,
    total_active_questions  INT          NOT NULL DEFAULT 0,
    answered_count          INT          NOT NULL DEFAULT 0,
    mandatory_gate_count    INT          NOT NULL DEFAULT 0,
    mandatory_gate_failed   INT          NOT NULL DEFAULT 0,
    evidence_pending_count  INT          NOT NULL DEFAULT 0,
    weighted_score          DECIMAL(9, 4),
    max_possible_score      DECIMAL(9, 4),
    failed_gate_codes       VARCHAR(1000),
    missing_question_codes  VARCHAR(1000),
    evaluation_json         LONGTEXT,
    evaluated_at            DATETIME(6)  NOT NULL,
    evaluated_by            VARCHAR(36),
    notes                   VARCHAR(1000),
    created_by              VARCHAR(36),
    created_at              DATETIME(6)  NOT NULL,
    updated_by              VARCHAR(36),
    updated_at              DATETIME(6),
    PRIMARY KEY (evaluation_id),
    INDEX idx_vqe_supplier_at (supplier_id, evaluated_at),
    CONSTRAINT fk_vqe_supplier FOREIGN KEY (supplier_id) REFERENCES supplier_master (supplier_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
