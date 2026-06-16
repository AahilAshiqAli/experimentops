package com.experimentops.platformapi.dal.repository;

import com.experimentops.platformapi.model.entity.Experiment;
import com.experimentops.platformapi.model.type.StatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExperimentRepository extends JpaRepository<Experiment, Long> {

    Optional<Experiment> findByUuidAndProjectUuidAndWorkspaceUuidAndEnabled(String uuid, String projectUuid, String workspaceUuid, boolean enabled);

    Optional<Experiment> findByUuidAndWorkspaceUuidAndEnabled(String uuid, String workspaceUuid, boolean enabled);

    Optional<Experiment> findByUuidAndProjectUuidAndWorkspaceUuidAndStatusAndEnabled(String uuid, String projectUuid, String workspaceUuid, StatusEnum status, boolean enabled);

    Optional<Experiment> findByNameAndProjectUuidAndWorkspaceUuidAndStatusAndEnabled(String name, String projectUuid, String workspaceUuid, StatusEnum status, boolean enabled);

    List<Experiment> findAllByProjectUuidAndWorkspaceUuidAndStatusAndEnabledOrderByCreationDateDesc(String projectUuid, String workspaceUuid, StatusEnum status, boolean enabled);

}
