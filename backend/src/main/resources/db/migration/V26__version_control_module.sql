-- M15 Version Control & Change Management (VER-01..VER-06)
CREATE TABLE cost_version_record (
    version_id CHAR(36)  NOT NULL,
    entity_type          VARCHAR(50)  NOT NULL,
    entity_id CHAR(36)  NOT NULL,
    version_number       INT          NOT NULL,
    change_trigger       VARCHAR(40)  NOT NULL,
    change_reference     VARCHAR(100),
    change_comment       VARCHAR(1000),
    snapshot_json        LONGTEXT     NOT NULL,
    program_baseline     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_by           VARCHAR(36),
    created_at           DATETIME(6)  NOT NULL,
    updated_by           VARCHAR(36),
    updated_at           DATETIME(6),
    PRIMARY KEY (version_id),
    UNIQUE KEY uk_cost_version_entity_num (entity_type, entity_id, version_number),
    INDEX idx_cost_version_entity (entity_type, entity_id, created_at),
    INDEX idx_cost_version_baseline (entity_type, entity_id, program_baseline)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
