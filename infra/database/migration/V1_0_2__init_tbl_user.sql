CREATE TABLE IF NOT EXISTS tbl_user (
    id                  SERIAL          PRIMARY KEY,
    uuid                VARCHAR(40)     NOT NULL,
    first_name          VARCHAR(100),
    last_name           VARCHAR(100),
    email               VARCHAR(150),
    user_role           VARCHAR(50),
    workspace_uuid      VARCHAR(40),
    status              INTEGER         NOT NULL DEFAULT 1,
    creation_date       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(40)     NOT NULL,
    last_updated        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by          VARCHAR(40)     NOT NULL,
    enabled             TINYINT(1)      NOT NULL DEFAULT 1,
    version             INT             NOT NULL DEFAULT 0
    );