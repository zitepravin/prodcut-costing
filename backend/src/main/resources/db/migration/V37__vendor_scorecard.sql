-- V37: Vendor Selection B4 — configurable scorecard templates and evaluation results
CREATE TABLE vendor_scorecard_template (
    template_id CHAR(36)  NOT NULL,
    template_code       VARCHAR(50)  NOT NULL,
    template_name       VARCHAR(200) NOT NULL,
    description         VARCHAR(1000),
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_by          VARCHAR(36),
    created_at          DATETIME(6)  NOT NULL,
    updated_by          VARCHAR(36),
    updated_at          DATETIME(6),
    PRIMARY KEY (template_id),
    UNIQUE KEY uk_scorecard_template_code (template_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE vendor_scorecard_category (
    category_id         VARCHAR(36)  NOT NULL,
    template_id CHAR(36)  NOT NULL,
    category_code       VARCHAR(40)  NOT NULL,
    category_name       VARCHAR(150) NOT NULL,
    weight_percent      DECIMAL(9, 4) NOT NULL,
    sequence_no         INT          NOT NULL DEFAULT 0,
    created_by          VARCHAR(36),
    created_at          DATETIME(6)  NOT NULL,
    updated_by          VARCHAR(36),
    updated_at          DATETIME(6),
    PRIMARY KEY (category_id),
    UNIQUE KEY uk_scorecard_cat (template_id, category_code),
    CONSTRAINT fk_vsc_template FOREIGN KEY (template_id) REFERENCES vendor_scorecard_template (template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE vendor_scorecard_result (
    result_id CHAR(36)  NOT NULL,
    template_id CHAR(36)  NOT NULL,
    supplier_id CHAR(36)  NOT NULL,
    bom_node_id CHAR(36),
    overall_score       DECIMAL(9, 4) NOT NULL,
    max_score           DECIMAL(9, 4) NOT NULL DEFAULT 100.0000,
    category_scores_json LONGTEXT,
    notes               VARCHAR(1000),
    scored_at           DATETIME(6)  NOT NULL,
    scored_by           VARCHAR(36),
    created_by          VARCHAR(36),
    created_at          DATETIME(6)  NOT NULL,
    updated_by          VARCHAR(36),
    updated_at          DATETIME(6),
    PRIMARY KEY (result_id),
    INDEX idx_vsr_supplier (supplier_id, scored_at),
    INDEX idx_vsr_template (template_id, scored_at),
    CONSTRAINT fk_vsr_template FOREIGN KEY (template_id) REFERENCES vendor_scorecard_template (template_id),
    CONSTRAINT fk_vsr_supplier FOREIGN KEY (supplier_id) REFERENCES supplier_master (supplier_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
