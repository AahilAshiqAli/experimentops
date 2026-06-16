CREATE TABLE IF NOT EXISTS tbl_experiment_runs (
    id                     SERIAL          PRIMARY KEY,
    uuid                   VARCHAR(40)     NOT NULL,
    experiment_uuid        VARCHAR(100)    NOT NULL,
    experiment_config_uuid VARCHAR(40)     NOT NULL,
    workspace_uuid         VARCHAR(40)     NOT NULL,
    experiment_status      INTEGER         NOT NULL DEFAULT 1,
    run_number             INTEGER        NOT NULL DEFAULT 1,
    creation_date          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by             VARCHAR(40)     NOT NULL,
    last_updated           TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by             VARCHAR(40)     NOT NULL,
    enabled                TINYINT(1)      NOT NULL DEFAULT 1,
    version                INT             NOT NULL DEFAULT 0
    );