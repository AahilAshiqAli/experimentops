CREATE TABLE IF NOT EXISTS tbl_project (
    id                  SERIAL          PRIMARY KEY,
    uuid                VARCHAR(40)     NOT NULL,
    name                VARCHAR(100)    NOT NULL,
    description         TEXT,
    workspace_uuid      VARCHAR(40)     NOT NULL,
    status              INTEGER         NOT NULL DEFAULT 1,
    creation_date       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(40)     NOT NULL,
    last_updated        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by          VARCHAR(40)     NOT NULL,
    enabled             TINYINT(1)      NOT NULL DEFAULT 1,
    version             INT             NOT NULL DEFAULT 0
    );