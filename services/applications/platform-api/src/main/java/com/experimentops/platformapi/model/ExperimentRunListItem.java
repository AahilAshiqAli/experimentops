package com.experimentops.platformapi.model;

import com.experimentops.platformapi.model.type.ExperimentStatusEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;

@RequiredArgsConstructor
@Getter
@Setter
public class ExperimentRunListItem implements ExperimentRunListItemProjection {

    private final String uuid;
    private final String name;
    private final Integer progress;
    private final Long datasetCount;
    private final ExperimentStatusEnum experimentStatus;
    private final Timestamp creationDate;
    private final Timestamp lastUpdated;
}
