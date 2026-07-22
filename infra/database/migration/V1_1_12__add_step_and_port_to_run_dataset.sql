SELECT COUNT(*) INTO @exist
FROM information_schema.columns
WHERE COLUMN_NAME = 'step_count'
  AND TABLE_NAME = 'tbl_dataset_runs'
  AND TABLE_SCHEMA = DATABASE()
    LIMIT 1;

SET @query = IF(
    @exist = 0,
    'ALTER TABLE tbl_dataset_runs ADD COLUMN step_count INT NULL',
    'SELECT ''Column already exists'' AS step_count'
);

PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @exist
FROM information_schema.columns
WHERE COLUMN_NAME = 'port_name'
  AND TABLE_NAME = 'tbl_dataset_runs'
  AND TABLE_SCHEMA = DATABASE()
    LIMIT 1;

SET @query = IF(
    @exist = 0,
    'ALTER TABLE tbl_dataset_runs ADD COLUMN port_name VARCHAR(100) NULL',
    'SELECT ''Column already exists'' AS port_name'
);

PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
