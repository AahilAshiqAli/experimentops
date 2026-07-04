SELECT COUNT(*) INTO @exist
FROM information_schema.columns
WHERE COLUMN_NAME = 'scan_status'
  AND TABLE_NAME = 'tbl_dataset_versions'
  AND TABLE_SCHEMA = DATABASE()
    LIMIT 1;

SET @query = IF(
    @exist = 0,
    'ALTER TABLE tbl_dataset_versions ADD COLUMN scan_status INT NOT NULL DEFAULT 0',
    'SELECT ''Column already updated'' status'
);

PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @exist
FROM information_schema.columns
WHERE COLUMN_NAME = 'scan_message'
  AND TABLE_NAME = 'tbl_dataset_versions'
  AND TABLE_SCHEMA = DATABASE()
    LIMIT 1;

SET @query = IF(
    @exist = 0,
    'ALTER TABLE tbl_dataset_versions ADD COLUMN scan_message TEXT NULL',
    'SELECT ''Column already updated'' status'
);

PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @exist
FROM information_schema.columns
WHERE COLUMN_NAME = 'preview_uri'
  AND TABLE_NAME = 'tbl_dataset_versions'
  AND TABLE_SCHEMA = DATABASE()
    LIMIT 1;

SET @query = IF(
    @exist = 0,
    'ALTER TABLE tbl_dataset_versions ADD COLUMN preview_uri VARCHAR(1024) NULL',
    'SELECT ''Column already updated'' status'
);

PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
