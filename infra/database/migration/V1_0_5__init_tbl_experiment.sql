CREATE TABLE IF NOT EXISTS tbl_experiment (
    id                  SERIAL          PRIMARY KEY,
    uuid                VARCHAR(40)     NOT NULL,
    name                VARCHAR(100),
    description         TEXT,
    experiment_type     VARCHAR(40)     NOT NUll,
    project_uuid        VARCHAR(40)     NOT NULL,
    workspace_uuid      VARCHAR(40)     NOT NULL,
    status              INTEGER         NOT NULL DEFAULT 1,
    creation_date       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(40)     NOT NULL,
    last_updated        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by          VARCHAR(40)     NOT NULL,
    enabled             TINYINT(1)      NOT NULL DEFAULT 1,
    version             INT             NOT NULL DEFAULT 0
    );