package com.experimentops.platformapi.model;

import com.experimentops.platformapi.model.type.ExperimentStatusEnum;

import java.sql.Timestamp;

public interface ExperimentRunListItemProjection {

    String getUuid();

    String getName();

    Integer getProgress();

    Long getDatasetCount();

    ExperimentStatusEnum getExperimentStatus();

    Timestamp getCreationDate();

    Timestamp getLastUpdated();

}
