SELECT COUNT(*) INTO @exist
FROM information_schema.columns
WHERE COLUMN_NAME = 'experiment_type'
  AND TABLE_NAME = 'tbl_experiment'
  AND TABLE_SCHEMA = DATABASE()
    LIMIT 1;

SET @query = IF(
    @exist > 0,
    'ALTER TABLE tbl_experiment DROP COLUMN experiment_type',
    'SELECT ''Column already updated'' experiment_type'
);

PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @exist
FROM information_schema.columns
WHERE COLUMN_NAME = 'experiment_type'
  AND TABLE_NAME = 'tbl_experiment_config'
  AND TABLE_SCHEMA = DATABASE()
    LIMIT 1;

SET @query = IF(
    @exist = 0,
    'ALTER TABLE tbl_experiment_config ADD COLUMN experiment_type VARCHAR(100) NULL AFTER name',
    'SELECT ''Column already updated'' experiment_config_experiment_type'
);

PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE tbl_experiment_config
SET experiment_type = name
WHERE experiment_type IS NULL;

SELECT COUNT(*) INTO @exist
FROM information_schema.columns
WHERE COLUMN_NAME = 'experiment_type'
  AND TABLE_NAME = 'tbl_experiment_config'
  AND TABLE_SCHEMA = DATABASE()
  AND IS_NULLABLE = 'YES'
    LIMIT 1;

SET @query = IF(
    @exist > 0,
    'ALTER TABLE tbl_experiment_config MODIFY COLUMN experiment_type VARCHAR(100) NOT NULL',
    'SELECT ''Column already updated'' experiment_config_experiment_type_nullable'
);

PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @exist
FROM information_schema.columns
WHERE COLUMN_NAME = 'execution_mode'
  AND TABLE_NAME = 'tbl_experiment_runs'
  AND TABLE_SCHEMA = DATABASE()
    LIMIT 1;

SET @query = IF(
    @exist = 0,
    'ALTER TABLE tbl_experiment_runs ADD COLUMN execution_mode JSON NULL',
    'SELECT ''Column already updated'' execution_mode'
);

PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @exist
FROM information_schema.columns
WHERE COLUMN_NAME = 'experiment_config_uuid'
  AND TABLE_NAME = 'tbl_experiment_runs'
  AND TABLE_SCHEMA = DATABASE()
    LIMIT 1;

SET @query = IF(
    @exist > 0,
    'UPDATE tbl_experiment_runs SET execution_mode = JSON_ARRAY(JSON_OBJECT(''stepCount'', 1, ''experimentConfigUuid'', experiment_config_uuid)) WHERE execution_mode IS NULL',
    'UPDATE tbl_experiment_runs SET execution_mode = JSON_ARRAY() WHERE execution_mode IS NULL'
);

PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @exist
FROM information_schema.columns
WHERE COLUMN_NAME = 'experiment_config_uuid'
  AND TABLE_NAME = 'tbl_experiment_runs'
  AND TABLE_SCHEMA = DATABASE()
    LIMIT 1;

SET @query = IF(
    @exist > 0,
    'ALTER TABLE tbl_experiment_runs DROP COLUMN experiment_config_uuid',
    'SELECT ''Column already updated'' experiment_config_uuid'
);

PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @exist
FROM information_schema.columns
WHERE COLUMN_NAME = 'execution_mode'
  AND TABLE_NAME = 'tbl_experiment_runs'
  AND TABLE_SCHEMA = DATABASE()
  AND IS_NULLABLE = 'YES'
    LIMIT 1;

SET @query = IF(
    @exist > 0,
    'ALTER TABLE tbl_experiment_runs MODIFY COLUMN execution_mode JSON NOT NULL',
    'SELECT ''Column already updated'' execution_mode'
);

PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
