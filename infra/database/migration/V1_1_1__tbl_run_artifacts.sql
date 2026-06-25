CREATE TABLE IF NOT EXISTS tbl_run_artifact (
    id                     SERIAL          PRIMARY KEY,
    uuid                   VARCHAR(40)     NOT NULL,
    experiment_run_uuid    VARCHAR(100)    NOT NULL,
    workspace_uuid         VARCHAR(40)     NOT NULL,
    artifact_type          VARCHAR(40)     NOT NULL,
    storage_uri            VARCHAR(1024)   NOT NULL,
    format                 VARCHAR(40)     NOT NULL,
    size_bytes             BIGINT,
    creation_date          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by             VARCHAR(40)     NOT NULL,
    last_updated           TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by             VARCHAR(40)     NOT NULL,
    enabled                TINYINT(1)      NOT NULL DEFAULT 1,
    version                INT             NOT NULL DEFAULT 0
    );
