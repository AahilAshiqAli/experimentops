package com.experimentops.platformapi.dal.repository;

import com.experimentops.platformapi.model.entity.ExperimentRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExperimentRunRepository extends JpaRepository<ExperimentRun, Long> {

    long countByExperimentUuidAndEnabled(String experimentUuid, boolean enabled);

}
