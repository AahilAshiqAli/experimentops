SELECT COUNT(*) INTO @exist
FROM information_schema.columns
WHERE COLUMN_NAME = 'step_completed'
  AND TABLE_NAME = 'tbl_experiment_runs'
  AND TABLE_SCHEMA = DATABASE()
    LIMIT 1;

SET @query = IF(
    @exist = 0,
    'ALTER TABLE tbl_experiment_runs ADD COLUMN step_completed INTEGER NULL',
    'SELECT ''Column already exists'' AS step_completed'
);

PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
