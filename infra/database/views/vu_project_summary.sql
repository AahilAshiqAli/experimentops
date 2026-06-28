CREATE OR REPLACE VIEW vu_project_summary AS
SELECT
    p.id            AS id,
    p.uuid          AS project_uuid,
    p.workspace_uuid AS workspace_uuid,
    p.name          AS name,
    p.description   AS description,
    p.status        AS status,
    p.enabled       AS enabled,
    p.creation_date AS created_at,

    COUNT(DISTINCT dv.uuid) AS dataset_version_count,
    COUNT(DISTINCT e.uuid)  AS experiment_count,
    COUNT(DISTINCT ec.uuid) AS experiment_config_count,
    COUNT(DISTINCT d.uuid)  AS dataset_count,
    COUNT(DISTINCT er.uuid) AS experiment_run_count

FROM `experimentOps`.tbl_project p

         LEFT JOIN `experimentOps`.tbl_dataset d
                   ON d.project_uuid = p.uuid
                       AND d.workspace_uuid = p.workspace_uuid
                       AND d.enabled = 1
                       AND d.status = 1

         LEFT JOIN `experimentOps`.tbl_dataset_versions dv
                   ON dv.dataset_uuid = d.uuid
                       AND d.workspace_uuid = p.workspace_uuid
                       AND d.enabled = 1
                       AND d.status = 1

         LEFT JOIN `experimentOps`.tbl_experiment e
                   ON e.project_uuid = p.uuid
                       AND e.workspace_uuid = p.workspace_uuid
                       AND e.enabled = 1
                       AND e.status = 1

         LEFT JOIN `experimentOps`.tbl_experiment_config ec
                   ON ec.experiment_uuid = e.uuid
                       AND ec.workspace_uuid = p.workspace_uuid
                       AND ec.enabled = 1
                       AND ec.status = 1

         LEFT JOIN `experimentOps`.tbl_experiment_runs er
                   ON er.experiment_uuid = e.uuid
                       AND er.workspace_uuid = p.workspace_uuid
                       AND er.enabled = 1


WHERE p.enabled = 1

GROUP BY
    p.id,
    p.uuid,
    p.workspace_uuid,
    p.name,
    p.description,
    p.status,
    p.enabled,
    p.creation_date;
