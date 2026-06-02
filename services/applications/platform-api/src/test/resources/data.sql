-- Pre-existing ACTIVE workspace used by duplicate-detection tests.
-- status=1 maps to StatusEnum.ACTIVE via ExperimentOpsEnumConverter.
INSERT INTO tbl_workspace (uuid, name, email, user_uuid, status, creation_date, created_by, last_updated, updated_by, enabled, version)
VALUES ('seed-workspace-uuid-001', 'Existing Workspace', 'existing@workspace.com', 'seed-user-uuid-001', 1, NOW(), '0', NOW(), '0', 1, 0);
