-- V56: Qualification APPROVED must reference a human-approved recommendation for the same supplier
-- Audit of gate checks (append-only)
CREATE TABLE supplier_qualification_approval_check (
    check_id            VARCHAR(36)  NOT NULL,
    supplier_id CHAR(36)  NOT NULL,
    qualification_id CHAR(36),
    recommendation_id CHAR(36)  NOT NULL,
    recommendation_status VARCHAR(30) NOT NULL,
    recommended_supplier_id CHAR(36),
    check_passed        BOOLEAN      NOT NULL,
    failure_reason      VARCHAR(1000),
    checked_at          DATETIME(6)  NOT NULL,
    checked_by          VARCHAR(36),
    created_by          VARCHAR(36),
    created_at          DATETIME(6)  NOT NULL,
    updated_by          VARCHAR(36),
    updated_at          DATETIME(6),
    PRIMARY KEY (check_id),
    INDEX idx_sqac_supplier (supplier_id, checked_at),
    INDEX idx_sqac_recommendation (recommendation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
