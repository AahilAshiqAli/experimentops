SELECT COUNT(*) INTO @exist
FROM information_schema.columns
WHERE COLUMN_NAME = 'time_weight'
  AND TABLE_NAME = 'tbl_experiment_types'
  AND TABLE_SCHEMA = DATABASE()
    LIMIT 1;

SET @query = IF(
    @exist = 0,
    'ALTER TABLE tbl_experiment_types
     ADD COLUMN time_weight INT NOT NULL DEFAULT 1',
    'SELECT ''Column already updated'' time_weight'
);

PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SELECT COUNT(*) INTO @exist
FROM information_schema.columns
WHERE COLUMN_NAME = 'format_mappings'
  AND TABLE_NAME = 'tbl_experiment_types'
  AND TABLE_SCHEMA = DATABASE()
    LIMIT 1;

SET @query = IF(
    @exist > 0,
    'UPDATE tbl_experiment_types
     SET format_mappings = JSON_OBJECT()
     WHERE format_mappings IS NULL',
    'SELECT ''Column does not exist'' AS format_mappings'
);

PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @is_nullable
FROM information_schema.columns
WHERE COLUMN_NAME = 'format_mappings'
  AND TABLE_NAME = 'tbl_experiment_types'
  AND TABLE_SCHEMA = DATABASE()
  AND IS_NULLABLE = 'YES';

SET @query = IF(
    @is_nullable > 0,
    'ALTER TABLE tbl_experiment_types
     MODIFY COLUMN format_mappings JSON NOT NULL',
    'SELECT ''Column already NOT NULL'' AS format_mappings'
);

PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
