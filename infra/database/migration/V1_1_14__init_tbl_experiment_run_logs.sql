CREATE TABLE IF NOT EXISTS tbl_experiment_run_logs (
    id                     SERIAL          PRIMARY KEY,
    uuid                   VARCHAR(40)     NOT NULL,
    experiment_run_uuid    VARCHAR(100)    NOT NULL,
    `sequence`             BIGINT          NOT NULL,
    `timestamp`            TIMESTAMP       NOT NULL,
    level                  VARCHAR(40)     NOT NULL,
    experiment_type        VARCHAR(100),
    message                TEXT,
    creation_date          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by             VARCHAR(40)     NOT NULL,
    last_updated           TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by             VARCHAR(40)     NOT NULL,
    enabled                TINYINT(1)      NOT NULL DEFAULT 1,
    version                INT             NOT NULL DEFAULT 0,
    INDEX idx_experiment_run_logs_run_sequence (experiment_run_uuid, `sequence`),
    INDEX idx_experiment_run_logs_run_timestamp (experiment_run_uuid, `timestamp`)
    );
