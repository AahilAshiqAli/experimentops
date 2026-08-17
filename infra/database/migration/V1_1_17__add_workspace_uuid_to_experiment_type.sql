SELECT COUNT(*) INTO @exist
FROM information_schema.columns
WHERE COLUMN_NAME = 'workspace_uuid'
  AND TABLE_NAME = 'tbl_experiment_types'
  AND TABLE_SCHEMA = DATABASE()
LIMIT 1;

SET @query = IF(
    @exist = 0,
    'ALTER TABLE tbl_experiment_types
     ADD COLUMN workspace_uuid VARCHAR(40) NULL',
    'SELECT ''Column already updated'' AS workspace_uuid'
);

PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
