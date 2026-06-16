package com.experimentops.platformapi.dal.repository;

import com.experimentops.platformapi.model.entity.ExperimentConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExperimentConfigRepository extends JpaRepository<ExperimentConfig, Long> {

    long countByExperimentUuidAndEnabled(String experimentUuid, boolean enabled);

}
