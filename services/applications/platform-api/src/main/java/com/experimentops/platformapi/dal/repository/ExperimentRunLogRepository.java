package com.experimentops.platformapi.dal.repository;

import com.experimentops.platformapi.model.entity.ExperimentRunLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExperimentRunLogRepository extends JpaRepository<ExperimentRunLog, Long> {

    Page<ExperimentRunLog> findByExperimentRunUuidAndEnabledOrderByTimestampAsc(
            String experimentRunUuid,
            boolean enabled,
            Pageable pageable
    );
}
