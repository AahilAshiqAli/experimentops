package com.experimentops.platformapi.dal.repository;

import com.experimentops.platformapi.model.entity.ExperimentType;
import com.experimentops.platformapi.model.type.StatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExperimentTypeRepository extends JpaRepository<ExperimentType, Long> {

    Optional<ExperimentType> findByUuidAndEnabled(String uuid, boolean enabled);

    Optional<ExperimentType> findByUuidAndStatusAndEnabled(String uuid, StatusEnum status, boolean enabled);

    Optional<ExperimentType> findByNameAndStatusAndEnabled(String name, StatusEnum status, boolean enabled);

    List<ExperimentType> findAllByStatusAndEnabledOrderByLastUpdatedDesc(StatusEnum status, boolean enabled);

}
