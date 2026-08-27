-- Manufacturing benchmark knowledge library (gap-report P0)
-- Internal/tenant-maintained rate packs — not a substitute for external global vendor libraries.

CREATE TABLE IF NOT EXISTS benchmark_pack (
    pack_id           CHAR(36)     NOT NULL PRIMARY KEY,
    pack_code         VARCHAR(40)  NOT NULL,
    pack_name         VARCHAR(200) NOT NULL,
    region_code       VARCHAR(20)  NOT NULL,
    source_label      VARCHAR(200) NULL,
    version_label     VARCHAR(40)  NOT NULL DEFAULT '1.0',
    status            VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    effective_from    DATE         NULL,
    effective_to      DATE         NULL,
    notes             VARCHAR(1000) NULL,
    created_by        VARCHAR(36)  NULL,
    created_at        TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by        VARCHAR(36)  NULL,
    updated_at        TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_benchmark_pack_code_ver (pack_code, version_label)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS benchmark_rate (
    rate_id           CHAR(36)     NOT NULL PRIMARY KEY,
    pack_id           CHAR(36)     NOT NULL,
    category          VARCHAR(30)  NOT NULL,
    process_family    VARCHAR(80)  NULL,
    rate_code         VARCHAR(40)  NOT NULL,
    description       VARCHAR(300) NULL,
    rate_value        DECIMAL(18,6) NOT NULL,
    currency_code     CHAR(3)      NOT NULL DEFAULT 'USD',
    unit_of_measure   VARCHAR(30)  NOT NULL,
    confidence        VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
    created_by        VARCHAR(36)  NULL,
    created_at        TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by        VARCHAR(36)  NULL,
    updated_at        TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_benchmark_rate_pack FOREIGN KEY (pack_id) REFERENCES benchmark_pack(pack_id),
    UNIQUE KEY uk_benchmark_rate_pack_code (pack_id, rate_code),
    KEY idx_benchmark_rate_cat (category, process_family)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
