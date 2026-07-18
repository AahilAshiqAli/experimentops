package com.experimentops.platformapi.dal.repository;

import com.experimentops.platformapi.model.entity.ExperimentConfig;
import com.experimentops.platformapi.model.type.StatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExperimentConfigRepository extends JpaRepository<ExperimentConfig, Long> {

    long countByExperimentUuidAndEnabled(String experimentUuid, boolean enabled);

    Optional<ExperimentConfig> findByUuidAndExperimentUuidAndWorkspaceUuidAndEnabled(String uuid, String experimentUuid, String workspaceUuid, boolean enabled);

    Optional<ExperimentConfig> findByUuidAndExperimentUuidAndWorkspaceUuidAndStatusAndEnabled(String uuid, String experimentUuid, String workspaceUuid, StatusEnum status, boolean enabled);

    Optional<ExperimentConfig> findByNameAndExperimentUuidAndWorkspaceUuidAndStatusAndEnabled(String name, String experimentUuid, String workspaceUuid, StatusEnum status, boolean enabled);

    Page<ExperimentConfig> findAllByExperimentUuidAndWorkspaceUuidAndStatusAndEnabledOrderByLastUpdatedDesc(String experimentUuid, String workspaceUuid, StatusEnum status, boolean enabled, Pageable pageable);

    Page<ExperimentConfig> findAllByExperimentUuidAndWorkspaceUuidAndExperimentTypeAndStatusAndEnabledOrderByLastUpdatedDesc(
            String experimentUuid,
            String workspaceUuid,
            String experimentType,
            StatusEnum status,
            boolean enabled,
            Pageable pageable
    );

    @Query(value = """
            SELECT
                ec.uuid AS experimentConfigUuid,
                ec.name AS name,
                ec.experiment_type AS experimentType,
                CAST(ec.config AS CHAR) AS config,
                CAST(et.format_mappings AS CHAR) AS formatMappings,
                et.time_weight AS timeWeight
            FROM tbl_experiment_config ec
            JOIN tbl_experiment_types et
                ON et.name = ec.experiment_type
               AND et.status = :experimentTypeStatus
               AND et.enabled = true
            WHERE ec.uuid IN (:experimentConfigUuids)
              AND ec.experiment_uuid = :experimentUuid
              AND ec.workspace_uuid = :workspaceUuid
              AND ec.status = :experimentTypeStatus
              AND ec.enabled = true
            """, nativeQuery = true)
    List<ExperimentConfigWithTypeProjection> findAllWithExperimentTypesByUuidIn(
            @Param("experimentConfigUuids") Collection<String> experimentConfigUuids,
            @Param("experimentUuid") String experimentUuid,
            @Param("workspaceUuid") String workspaceUuid,
            @Param("experimentTypeStatus") int experimentTypeStatus
    );

}
