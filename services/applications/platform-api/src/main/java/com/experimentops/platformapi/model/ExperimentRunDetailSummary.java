package com.experimentops.platformapi.model;

import com.experimentops.platformapi.model.entity.ExecutionMode;
import com.experimentops.platformapi.model.type.ExperimentStatusEnum;

import java.sql.Timestamp;
import java.util.List;

public record ExperimentRunDetailSummary(
        String uuid,
        String name,
        ExperimentStatusEnum experimentStatus,
        String message,
        String experimentUuid,
        String experimentName,
        String projectUuid,
        String projectName,
        Timestamp creationDate,
        Timestamp lastUpdated,
        String createdBy,
        Long artifactCount,
        List<ExecutionMode> executionMode
) {
    public ExperimentRunDetailSummary {
        executionMode = executionMode == null ? List.of() : executionMode;
    }
}
