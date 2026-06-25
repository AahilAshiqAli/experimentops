CREATE TABLE IF NOT EXISTS tbl_dataset (
    id                  SERIAL          PRIMARY KEY,
    uuid                VARCHAR(40)     NOT NULL,
    name                VARCHAR(100)    NOT NULL,
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

CREATE TABLE IF NOT EXISTS tbl_dataset_versions (
    id                  SERIAL          PRIMARY KEY,
    uuid                VARCHAR(40)     NOT NULL,
    original_name       VARCHAR(100)    NOT NULL,
    storage_uri         VARCHAR(1024)   NOT NULL,
    format              VARCHAR(40),
    size_bytes          BIGINT,
    dataset_uuid        VARCHAR(40)     NOT NULL,
    workspace_uuid      VARCHAR(40)     NOT NULL,
    status              INTEGER         NOT NULL DEFAULT 1,
    creation_date       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(40)     NOT NULL,
    last_updated        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by          VARCHAR(40)     NOT NULL,
    enabled             TINYINT(1)      NOT NULL DEFAULT 1,
    version             INT             NOT NULL DEFAULT 0
    );

CREATE TABLE IF NOT EXISTS tbl_dataset_runs (
    id                   SERIAL          PRIMARY KEY,
    uuid                 VARCHAR(40)     NOT NULL,
    experiment_run_uuid  VARCHAR(40)     NOT NULL,
    dataset_version_uuid VARCHAR(40)       NOT NULL,
    workspace_uuid       VARCHAR(40)     NOT NULL,
    usage_type           VARCHAR(40)     NOT NULL,
    creation_date        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by           VARCHAR(40)     NOT NULL,
    last_updated         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by           VARCHAR(40)     NOT NULL,
    enabled              TINYINT(1)      NOT NULL DEFAULT 1,
    version              INT             NOT NULL DEFAULT 0
    );
