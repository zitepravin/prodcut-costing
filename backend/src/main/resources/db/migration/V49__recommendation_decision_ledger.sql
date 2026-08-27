-- V49: Append-only recommendation decision ledger (human decisions; system never auto-approves)
CREATE TABLE vendor_recommendation_decision (
    decision_id CHAR(36)  NOT NULL,
    recommendation_id CHAR(36)  NOT NULL,
    explanation_id CHAR(36),
    previous_status         VARCHAR(30)  NOT NULL,
    new_status              VARCHAR(30)  NOT NULL,
    decision_action         VARCHAR(30)  NOT NULL,
    recommended_supplier_id CHAR(36),
    override_supplier_id    VARCHAR(36),
    decision_comment        VARCHAR(2000),
    override_reason         VARCHAR(2000),
    decided_by              VARCHAR(36),
    decided_at              DATETIME(6)  NOT NULL,
    created_by              VARCHAR(36),
    created_at              DATETIME(6)  NOT NULL,
    updated_by              VARCHAR(36),
    updated_at              DATETIME(6),
    PRIMARY KEY (decision_id),
    INDEX idx_vrd_recommendation (recommendation_id, decided_at),
    INDEX idx_vrd_action (decision_action, decided_at),
    CONSTRAINT fk_vrd_recommendation FOREIGN KEY (recommendation_id)
        REFERENCES vendor_recommendation (recommendation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
