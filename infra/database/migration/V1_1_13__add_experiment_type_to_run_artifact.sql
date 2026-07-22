SELECT COUNT(*) INTO @exist
FROM information_schema.columns
WHERE COLUMN_NAME = 'experiment_type'
  AND TABLE_NAME = 'tbl_run_artifact'
  AND TABLE_SCHEMA = DATABASE()
    LIMIT 1;

SET @query = IF(
    @exist = 0,
    'ALTER TABLE tbl_run_artifact ADD COLUMN experiment_type VARCHAR(100) NULL',
    'SELECT ''Column already exists'' AS experiment_type'
);

PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
