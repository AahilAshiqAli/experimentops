package com.experimentops.platformapi.model;

import com.experimentops.platformapi.model.type.StatusEnum;

import java.sql.Timestamp;

public interface ExperimentListItemProjection {

    String getExperimentUuid();

    String getName();

    String getDescription();

    String getExperimentType();

    StatusEnum getStatus();

    Timestamp getCreationDate();

    Long getConfigCount();

    Long getRunCount();

}
