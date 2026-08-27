-- V48: Structured recommendation explainability snapshot (decision support — never auto-approval)
-- Separates FACT / CALCULATED / INFERENCE / MISSING DATA for human review.
CREATE TABLE vendor_recommendation_explanation (
    explanation_id CHAR(36)  NOT NULL,
    recommendation_id CHAR(36)  NOT NULL,
    recommended_supplier_id CHAR(36),
    confidence_level        VARCHAR(20),
    data_used_summary       TEXT,
    eligibility_summary     TEXT,
    score_summary           TEXT,
    tco_summary             TEXT,
    strengths_summary       TEXT,
    weaknesses_summary      TEXT,
    risk_summary            TEXT,
    missing_data_summary    TEXT,
    cost_variance_notes     TEXT,
    evidence_notes          TEXT,
    decision_boundary_note  TEXT NOT NULL,
    fact_json               LONGTEXT,
    calculated_json         LONGTEXT,
    inference_json          LONGTEXT,
    missing_json            LONGTEXT,
    created_by              VARCHAR(36),
    created_at              DATETIME(6)  NOT NULL,
    updated_by              VARCHAR(36),
    updated_at              DATETIME(6),
    PRIMARY KEY (explanation_id),
    UNIQUE KEY uk_vre_recommendation (recommendation_id),
    CONSTRAINT fk_vre_recommendation FOREIGN KEY (recommendation_id)
        REFERENCES vendor_recommendation (recommendation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
