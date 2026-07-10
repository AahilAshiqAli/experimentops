SELECT COUNT(*) INTO @exist
FROM information_schema.columns
WHERE COLUMN_NAME = 'format_mappings'
  AND TABLE_NAME = 'tbl_experiment_types'
  AND TABLE_SCHEMA = DATABASE()
    LIMIT 1;

SET @query = IF(
    @exist = 0,
    'ALTER TABLE tbl_experiment_types
     ADD COLUMN format_mappings JSON NULL',
    'SELECT ''Column already updated'' format_mappings'
);

PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
