package com.experimentops.platformapi.model;

import com.experimentops.platformapi.model.entity.ExecutionMode;
import com.experimentops.platformapi.model.type.ExperimentStatusEnum;

import java.sql.Timestamp;
import java.util.List;

public interface ExperimentRunListItemProjection {

    String getUuid();

    String getName();

    Integer getProgress();

    Long getDatasetCount();

    ExperimentStatusEnum getExperimentStatus();

    List<ExecutionMode> getExecutionMode();

    Timestamp getCreationDate();

    Timestamp getLastUpdated();

}
