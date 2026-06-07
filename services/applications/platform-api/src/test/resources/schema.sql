DROP TABLE IF EXISTS tbl_workspace;

CREATE TABLE IF NOT EXISTS tbl_workspace (
    id                  SERIAL          PRIMARY KEY,
    uuid                VARCHAR(40)     NOT NULL,
    name                VARCHAR(150),
    email               VARCHAR(150),
    user_uuid           VARCHAR(40),
    status              INTEGER         NOT NULL DEFAULT 1,
    creation_date       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(40)     NOT NULL,
    last_updated        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by          VARCHAR(40)     NOT NULL,
    enabled             TINYINT(1)      NOT NULL DEFAULT 1,
    version             INT             NOT NULL DEFAULT 0
);

DROP TABLE IF EXISTS tbl_notification;

CREATE TABLE IF NOT EXISTS tbl_notification (
    id                  SERIAL          PRIMARY KEY,
    uuid                VARCHAR(40)     NOT NULL,
    event_type          VARCHAR(100)    NOT NULL,
    workspace_uuid      VARCHAR(40),
    recipient           VARCHAR(150)    NOT NULL,
    provider            INTEGER         NOT NULL,
    status              INTEGER         NOT NULL DEFAULT 0,
    failure_reason      VARCHAR(1000),
    retry_count         INT             NOT NULL DEFAULT 0,
    creation_date       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(40)     NOT NULL,
    last_updated        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by          VARCHAR(40)     NOT NULL,
    enabled             TINYINT(1)      NOT NULL DEFAULT 1,
    version             INT             NOT NULL DEFAULT 0,
    UNIQUE KEY uk_tbl_notification_uuid (uuid)
);
