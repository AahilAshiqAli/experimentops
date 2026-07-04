package com.experimentops.platformapi.dal.repository;

import com.experimentops.platformapi.model.entity.ExperimentConfig;
import com.experimentops.platformapi.model.type.StatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExperimentConfigRepository extends JpaRepository<ExperimentConfig, Long> {

    long countByExperimentUuidAndEnabled(String experimentUuid, boolean enabled);

    Optional<ExperimentConfig> findByUuidAndExperimentUuidAndWorkspaceUuidAndEnabled(String uuid, String experimentUuid, String workspaceUuid, boolean enabled);

    Optional<ExperimentConfig> findByUuidAndExperimentUuidAndWorkspaceUuidAndStatusAndEnabled(String uuid, String experimentUuid, String workspaceUuid, StatusEnum status, boolean enabled);

    Optional<ExperimentConfig> findByNameAndExperimentUuidAndWorkspaceUuidAndStatusAndEnabled(String name, String experimentUuid, String workspaceUuid, StatusEnum status, boolean enabled);

    Page<ExperimentConfig> findAllByExperimentUuidAndWorkspaceUuidAndStatusAndEnabledOrderByLastUpdatedDesc(String experimentUuid, String workspaceUuid, StatusEnum status, boolean enabled, Pageable pageable);

}
