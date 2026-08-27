-- M12 Currency Management (SRS Currency / multi-currency support)
-- Append-only exchange rates; conversion is always from_currency -> to_currency via
-- a dated rate. Rates are never updated in place (same MDM-02 / commodity-index pattern).

CREATE TABLE exchange_rate (
    rate_id          CHAR(36)      NOT NULL PRIMARY KEY,
    from_currency    CHAR(3)       NOT NULL,
    to_currency      CHAR(3)       NOT NULL,
    rate             NUMERIC(18,8) NOT NULL,
    effective_date   DATE          NOT NULL,
    source           VARCHAR(100),
    created_by       VARCHAR(36),
    created_at       DATETIME(6)   NOT NULL,
    updated_by       VARCHAR(36),
    updated_at       DATETIME(6),
    CONSTRAINT fk_exchange_rate_from FOREIGN KEY (from_currency) REFERENCES currency (currency_code),
    CONSTRAINT fk_exchange_rate_to   FOREIGN KEY (to_currency)   REFERENCES currency (currency_code),
    CONSTRAINT chk_exchange_rate_positive CHECK (rate > 0),
    CONSTRAINT chk_exchange_rate_diff_ccy CHECK (from_currency <> to_currency)
) ENGINE = InnoDB;

-- idempotent: idx_exchange_rate_pair_date
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'exchange_rate' AND index_name = 'idx_exchange_rate_pair_date'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_exchange_rate_pair_date ON exchange_rate (from_currency, to_currency, effective_date)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
-- idempotent: idx_exchange_rate_effective
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'exchange_rate' AND index_name = 'idx_exchange_rate_effective'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_exchange_rate_effective ON exchange_rate (effective_date)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Seed common ISO currencies if not already present (V2 may have left the table empty)
INSERT IGNORE INTO currency (currency_code, description, decimal_places) VALUES
    ('USD', 'US Dollar', 2),
    ('EUR', 'Euro', 2),
    ('JPY', 'Japanese Yen', 0),
    ('CNY', 'Chinese Yuan', 2),
    ('INR', 'Indian Rupee', 2),
    ('GBP', 'British Pound', 2),
    ('KRW', 'South Korean Won', 0),
    ('MXN', 'Mexican Peso', 2),
    ('BRL', 'Brazilian Real', 2),
    ('THB', 'Thai Baht', 2);

-- Seed illustrative rates (effective today) for local demo; production loads from treasury feed
INSERT INTO exchange_rate (rate_id, from_currency, to_currency, rate, effective_date, source, created_at)
VALUES
    (UUID(), 'USD', 'EUR', 0.92000000, CURDATE(), 'SEED', UTC_TIMESTAMP(6)),
    (UUID(), 'EUR', 'USD', 1.08700000, CURDATE(), 'SEED', UTC_TIMESTAMP(6)),
    (UUID(), 'USD', 'INR', 83.50000000, CURDATE(), 'SEED', UTC_TIMESTAMP(6)),
    (UUID(), 'INR', 'USD', 0.01197600, CURDATE(), 'SEED', UTC_TIMESTAMP(6)),
    (UUID(), 'USD', 'JPY', 149.50000000, CURDATE(), 'SEED', UTC_TIMESTAMP(6)),
    (UUID(), 'JPY', 'USD', 0.00668900, CURDATE(), 'SEED', UTC_TIMESTAMP(6)),
    (UUID(), 'USD', 'CNY', 7.25000000, CURDATE(), 'SEED', UTC_TIMESTAMP(6)),
    (UUID(), 'CNY', 'USD', 0.13793100, CURDATE(), 'SEED', UTC_TIMESTAMP(6)),
    (UUID(), 'EUR', 'INR', 90.76000000, CURDATE(), 'SEED', UTC_TIMESTAMP(6)),
    (UUID(), 'INR', 'EUR', 0.01101800, CURDATE(), 'SEED', UTC_TIMESTAMP(6));
