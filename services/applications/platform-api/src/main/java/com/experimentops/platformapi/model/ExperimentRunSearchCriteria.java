package com.experimentops.platformapi.model;

import com.experimentops.platformapi.model.type.ExperimentStatusEnum;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

public record ExperimentRunSearchCriteria(@NonNull String experimentUuid, @Nullable String name,
                                          @Nullable List<ExperimentStatusEnum> statuses, @NonNull String workspaceUuid,
                                          @Nullable String userUuid) {

}
