-- V165: Customer company logo file reference (TC-012)
-- Bytes live in stored_file / object storage. platform_customer holds only the file id.
-- Idempotent: skip if column already exists.

SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE table_schema = DATABASE()
    AND table_name = 'platform_customer'
    AND column_name = 'logo_file_id'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE platform_customer ADD COLUMN logo_file_id CHAR(36) NULL',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
