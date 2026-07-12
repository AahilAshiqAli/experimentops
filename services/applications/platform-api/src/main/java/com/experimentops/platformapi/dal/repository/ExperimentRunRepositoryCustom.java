package com.experimentops.platformapi.dal.repository;

import com.experimentops.platformapi.model.ExperimentRunListItemProjection;
import com.experimentops.platformapi.model.ExperimentRunSearchCriteria;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ExperimentRunRepositoryCustom {

    @NonNull
    Page<ExperimentRunListItemProjection> searchExperimentRuns(
            @NonNull ExperimentRunSearchCriteria criteria,
            @NonNull Pageable pageable
    );
}
