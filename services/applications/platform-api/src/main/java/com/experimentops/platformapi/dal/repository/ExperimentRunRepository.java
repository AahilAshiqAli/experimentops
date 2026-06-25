package com.experimentops.platformapi.dal.repository;

import com.experimentops.platformapi.model.entity.ExperimentRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExperimentRunRepository extends JpaRepository<ExperimentRun, Long> {

    long countByExperimentUuidAndEnabled(String experimentUuid, boolean enabled);

    Optional<ExperimentRun> findByUuidAndWorkspaceUuidAndEnabled(String uuid, String workspaceUuid, boolean enabled);

}
