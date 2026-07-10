package com.experimentops.platformapi.dal.repository;

public interface ExperimentConfigWithTypeProjection {
    String getExperimentConfigUuid();

    String getExperimentType();

    String getConfig();

    String getFormatMappings();
}
