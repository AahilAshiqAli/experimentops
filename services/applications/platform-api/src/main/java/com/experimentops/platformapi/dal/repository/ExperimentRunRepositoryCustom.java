package com.experimentops.platformapi.dal.repository;

import com.experimentops.platformapi.model.ExperimentRunDetailArtifact;
import com.experimentops.platformapi.model.ExperimentRunDetailConfigType;
import com.experimentops.platformapi.model.ExperimentRunDetailDataset;
import com.experimentops.platformapi.model.ExperimentRunDetailSummary;
import com.experimentops.platformapi.model.ExperimentRunListItemProjection;
import com.experimentops.platformapi.model.ExperimentRunSearchCriteria;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ExperimentRunRepositoryCustom {

    @NonNull
    Page<ExperimentRunListItemProjection> searchExperimentRuns(
            @NonNull ExperimentRunSearchCriteria criteria,
            @NonNull Pageable pageable
    );

    @NonNull
    Optional<ExperimentRunDetailSummary> findExperimentRunDetailSummary(
            @NonNull String experimentRunUuid,
            @NonNull String workspaceUuid
    );

    @NonNull
    List<ExperimentRunDetailDataset> findExperimentRunDetailDatasets(
            @NonNull String experimentRunUuid,
            @NonNull String workspaceUuid
    );

    @NonNull
    List<ExperimentRunDetailArtifact> findExperimentRunDetailPrimaryArtifacts(
            @NonNull String experimentRunUuid,
            @NonNull String workspaceUuid
    );

    @NonNull
    List<ExperimentRunDetailConfigType> findExperimentRunDetailConfigTypes(
            @NonNull Collection<String> experimentConfigUuids,
            @NonNull String experimentUuid,
            @NonNull String workspaceUuid
    );
}
