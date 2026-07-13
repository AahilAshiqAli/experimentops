package com.experimentops.platformapi.model;

import com.experimentops.platformapi.model.entity.ExecutionMode;
import com.experimentops.platformapi.model.type.ExperimentStatusEnum;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;

@Getter
public class ExperimentRunListItem implements ExperimentRunListItemProjection {

    private final String uuid;
    private final String name;
    private final Integer progress;
    private final Long datasetCount;
    private final ExperimentStatusEnum experimentStatus;
    private final List<ExecutionMode> executionMode;
    private final Timestamp creationDate;
    private final Timestamp lastUpdated;

    public ExperimentRunListItem(
            @NonNull String uuid,
            @Nullable String name,
            @Nullable Integer progress,
            @NonNull Long datasetCount,
            @NonNull ExperimentStatusEnum experimentStatus,
            @Nullable Object executionMode,
            @NonNull Timestamp creationDate,
            @NonNull Timestamp lastUpdated) {

        this.uuid = uuid;
        this.name = name;
        this.progress = progress;
        this.datasetCount = datasetCount;
        this.experimentStatus = experimentStatus;
        this.executionMode = toExecutionModeList(executionMode);
        this.creationDate = creationDate;
        this.lastUpdated = lastUpdated;
    }

    @NonNull
    @SuppressWarnings("unchecked")
    private List<ExecutionMode> toExecutionModeList(@Nullable Object executionMode) {
        if (executionMode instanceof List<?>) {
            return (List<ExecutionMode>) executionMode;
        }
        return Collections.emptyList();
    }
}
