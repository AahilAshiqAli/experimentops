CREATE TABLE IF NOT EXISTS tbl_user_reset_password (
    id                  SERIAL          PRIMARY KEY,
    uuid                VARCHAR(40)     NOT NULL,
    user_uuid           VARCHAR(40)     NOT NULL,
    workspace_name      VARCHAR(100)    NOT NULL,
    email               VARCHAR(150)    NOT NULL,
    workspace_uuid      VARCHAR(40)     NOT NULL,
    creation_date       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(40)     NOT NULL,
    last_updated        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by          VARCHAR(40)     NOT NULL,
    enabled             TINYINT(1)      NOT NULL DEFAULT 1,
    version             INT             NOT NULL DEFAULT 0
    );