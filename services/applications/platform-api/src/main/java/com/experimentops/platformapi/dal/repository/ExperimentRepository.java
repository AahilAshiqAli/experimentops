package com.experimentops.platformapi.dal.repository;

import com.experimentops.platformapi.model.entity.Experiment;
import com.experimentops.platformapi.model.ExperimentListItemProjection;
import com.experimentops.platformapi.model.type.StatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExperimentRepository extends JpaRepository<Experiment, Long> {

    Optional<Experiment> findByUuidAndProjectUuidAndWorkspaceUuidAndEnabled(String uuid, String projectUuid, String workspaceUuid, boolean enabled);

    Optional<Experiment> findByUuidAndWorkspaceUuidAndEnabled(String uuid, String workspaceUuid, boolean enabled);

    Optional<Experiment> findByUuidAndWorkspaceUuidAndStatusAndEnabled(String uuid, String workspaceUuid, StatusEnum status, boolean enabled);

    Optional<Experiment> findByUuidAndProjectUuidAndWorkspaceUuidAndStatusAndEnabled(String uuid, String projectUuid, String workspaceUuid, StatusEnum status, boolean enabled);

    Optional<Experiment> findByNameAndProjectUuidAndWorkspaceUuidAndStatusAndEnabled(String name, String projectUuid, String workspaceUuid, StatusEnum status, boolean enabled);

    @Query(value = """
            SELECT
                e.uuid AS experimentUuid,
                e.name AS name,
                e.description AS description,
                e.experimentType AS experimentType,
                e.status AS status,
                e.creationDate AS creationDate,
                (
                    SELECT COUNT(ec)
                    FROM ExperimentConfig ec
                    WHERE ec.experimentUuid = e.uuid
                      AND ec.enabled = true
                ) AS configCount,
                (
                    SELECT COUNT(er)
                    FROM ExperimentRun er
                    WHERE er.experimentUuid = e.uuid
                      AND er.enabled = true
                ) AS runCount
            FROM Experiment e
            WHERE e.projectUuid = :projectUuid
              AND e.workspaceUuid = :workspaceUuid
              AND e.status = :status
              AND e.enabled = true
            ORDER BY e.creationDate DESC
            """,
            countQuery = """
            SELECT COUNT(e)
            FROM Experiment e
            WHERE e.projectUuid = :projectUuid
              AND e.workspaceUuid = :workspaceUuid
              AND e.status = :status
              AND e.enabled = true
            """)
    Page<ExperimentListItemProjection> findExperimentListItems(
            @Param("projectUuid") String projectUuid,
            @Param("workspaceUuid") String workspaceUuid,
            @Param("status") StatusEnum status,
            Pageable pageable
    );

}
