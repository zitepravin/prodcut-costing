-- V41: Supplier qualification lifecycle (separate from operational supplier_master.status)
CREATE TABLE supplier_qualification (
    qualification_id CHAR(36)  NOT NULL,
    supplier_id CHAR(36)  NOT NULL,
    lifecycle_status    VARCHAR(30)  NOT NULL,
    previous_status     VARCHAR(30),
    change_reason       VARCHAR(1000),
    recommendation_id CHAR(36),
    effective_at        DATETIME(6)  NOT NULL,
    changed_by          VARCHAR(36),
    created_by          VARCHAR(36),
    created_at          DATETIME(6)  NOT NULL,
    updated_by          VARCHAR(36),
    updated_at          DATETIME(6),
    PRIMARY KEY (qualification_id),
    INDEX idx_sqf_supplier (supplier_id, effective_at),
    INDEX idx_sqf_status (lifecycle_status, effective_at),
    CONSTRAINT fk_sqf_supplier FOREIGN KEY (supplier_id) REFERENCES supplier_master (supplier_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE supplier_qualification_current (
    supplier_id CHAR(36)  NOT NULL,
    lifecycle_status    VARCHAR(30)  NOT NULL,
    qualification_id CHAR(36)  NOT NULL,
    updated_at          DATETIME(6)  NOT NULL,
    PRIMARY KEY (supplier_id),
    CONSTRAINT fk_sqc_supplier FOREIGN KEY (supplier_id) REFERENCES supplier_master (supplier_id),
    CONSTRAINT fk_sqc_qual FOREIGN KEY (qualification_id) REFERENCES supplier_qualification (qualification_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
