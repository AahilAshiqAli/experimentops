SELECT COUNT(*) INTO @exist
FROM information_schema.columns
WHERE COLUMN_NAME = 'storage_uri'
  AND TABLE_NAME = 'tbl_dataset_versions'
  AND TABLE_SCHEMA = DATABASE()
  AND CHARACTER_MAXIMUM_LENGTH < 1024
    LIMIT 1;

SET @query = IF(
    @exist > 0,
    'ALTER TABLE tbl_dataset_versions MODIFY COLUMN storage_uri VARCHAR(1024) NOT NULL',
    'SELECT ''Column already updated'' status'
);

PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @exist
FROM information_schema.columns
WHERE COLUMN_NAME = 'progress'
  AND TABLE_NAME = 'tbl_experiment_runs'
  AND TABLE_SCHEMA = DATABASE()
    LIMIT 1;

SET @query = IF(
    @exist = 0,
    'ALTER TABLE tbl_experiment_runs ADD COLUMN progress INT NOT NULL',
    'SELECT ''Column already updated'' status'
);

PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;