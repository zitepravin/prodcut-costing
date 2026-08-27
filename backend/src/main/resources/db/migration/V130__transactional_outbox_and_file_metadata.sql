-- V130: Production hardening foundations
-- 1) Transactional Outbox for reliable domain event publishing
-- 2) File metadata table for object-storage migration (no large blobs in MySQL)
--
-- Rationale:
-- - Prevent "DB commit succeeded but notification/integration lost"
-- - Stop storing large files/images/PDFs/Excel inside MySQL LOBs or app disk
-- - Tenant isolation on file metadata via customer_id

-- ---------------------------------------------------------------------------
-- Transactional Outbox
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS domain_outbox (
    outbox_id           CHAR(36)       NOT NULL PRIMARY KEY,
    customer_id         CHAR(36)       NULL,
    aggregate_type      VARCHAR(80)    NOT NULL,
    aggregate_id        CHAR(36)       NOT NULL,
    event_type          VARCHAR(120)   NOT NULL,
    payload_json        JSON           NOT NULL,
    correlation_id      VARCHAR(64)    NULL,
    status              VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    attempt_count       INT            NOT NULL DEFAULT 0,
    max_attempts        INT            NOT NULL DEFAULT 10,
    available_at        DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    locked_by           VARCHAR(80)    NULL,
    locked_at           DATETIME(6)    NULL,
    processed_at        DATETIME(6)    NULL,
    last_error          VARCHAR(1000)  NULL,
    created_at          DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)    NULL,
    CONSTRAINT chk_outbox_status CHECK (status IN ('PENDING', 'PROCESSING', 'PROCESSED', 'FAILED', 'DEAD'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Worker claim / poll path
CREATE INDEX idx_outbox_claim
    ON domain_outbox (status, available_at, created_at);

CREATE INDEX idx_outbox_customer_created
    ON domain_outbox (customer_id, created_at);

CREATE INDEX idx_outbox_aggregate
    ON domain_outbox (aggregate_type, aggregate_id);

CREATE INDEX idx_outbox_correlation
    ON domain_outbox (correlation_id);

-- ---------------------------------------------------------------------------
-- File metadata (object storage pointer)
-- Actual bytes live in object storage; MySQL holds only metadata.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS stored_file (
    file_id             CHAR(36)       NOT NULL PRIMARY KEY,
    customer_id         CHAR(36)       NOT NULL,
    entity_type         VARCHAR(80)    NOT NULL,
    entity_id           CHAR(36)       NULL,
    object_key          VARCHAR(512)   NOT NULL,
    file_name           VARCHAR(255)   NOT NULL,
    content_type        VARCHAR(120)   NULL,
    size_bytes          BIGINT         NOT NULL DEFAULT 0,
    checksum_sha256     VARCHAR(64)    NULL,
    storage_provider    VARCHAR(40)    NOT NULL DEFAULT 'LOCAL',
    status              VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    created_by          VARCHAR(36)    NULL,
    created_at          DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by          VARCHAR(36)    NULL,
    updated_at          DATETIME(6)    NULL,
    CONSTRAINT uk_stored_file_object_key UNIQUE (object_key),
    CONSTRAINT chk_stored_file_status CHECK (status IN ('ACTIVE', 'DELETED', 'PENDING_UPLOAD', 'FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_stored_file_customer_entity
    ON stored_file (customer_id, entity_type, entity_id);

CREATE INDEX idx_stored_file_customer_created
    ON stored_file (customer_id, created_at);

-- Note: customer_id is NOT NULL for stored_file. Platform-level files must still
-- carry a synthetic platform tenant id if ever needed; never leave tenant blank.
