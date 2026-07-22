SELECT COUNT(*) INTO @exist
FROM information_schema.columns
WHERE COLUMN_NAME = 'status'
  AND TABLE_NAME = 'tbl_run_artifact'
  AND TABLE_SCHEMA = DATABASE()
    LIMIT 1;

SET @query = IF(
    @exist = 0,
    'ALTER TABLE tbl_run_artifact ADD COLUMN status VARCHAR(40) NOT NULL',
    'SELECT ''Column already exists'' AS status'
);

PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @exist
FROM information_schema.columns
WHERE COLUMN_NAME = 'downstream_policy'
  AND TABLE_NAME = 'tbl_run_artifact'
  AND TABLE_SCHEMA = DATABASE()
    LIMIT 1;

SET @query = IF(
    @exist = 0,
    'ALTER TABLE tbl_run_artifact ADD COLUMN downstream_policy VARCHAR(40) NOT NULL',
    'SELECT ''Column already exists'' AS downstream_policy'
);

PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
