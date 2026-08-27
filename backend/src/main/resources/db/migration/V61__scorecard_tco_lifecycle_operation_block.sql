-- V61: Block new scorecard/TCO calculations when supplier lifecycle is BLOCKED or SUSPENDED
CREATE TABLE supplier_lifecycle_operation_block (
    block_id            VARCHAR(36)  NOT NULL,
    supplier_id CHAR(36)  NOT NULL,
    operation_code      VARCHAR(40)  NOT NULL,
    lifecycle_status    VARCHAR(30)  NOT NULL,
    block_reason        VARCHAR(1000) NOT NULL,
    attempted_at        DATETIME(6)  NOT NULL,
    attempted_by        VARCHAR(36),
    created_by          VARCHAR(36),
    created_at          DATETIME(6)  NOT NULL,
    updated_by          VARCHAR(36),
    updated_at          DATETIME(6),
    PRIMARY KEY (block_id),
    INDEX idx_slob_supplier (supplier_id, attempted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
