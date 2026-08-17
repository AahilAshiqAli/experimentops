SELECT CHARACTER_MAXIMUM_LENGTH INTO @experiment_type_length
FROM information_schema.columns
WHERE COLUMN_NAME = 'experiment_type'
  AND TABLE_NAME = 'tbl_experiment_run_logs'
  AND TABLE_SCHEMA = DATABASE()
LIMIT 1;

SET @query = IF(
    @experiment_type_length IS NOT NULL AND @experiment_type_length < 255,
    'ALTER TABLE tbl_experiment_run_logs MODIFY COLUMN experiment_type VARCHAR(255) NULL',
    'SELECT ''Column already widened'' AS experiment_type'
);

PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
