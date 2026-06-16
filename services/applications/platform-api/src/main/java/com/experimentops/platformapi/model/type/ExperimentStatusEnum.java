package com.experimentops.platformapi.model.type;

import com.experimentops.utils.model.enums.ExperimentOpsEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.Strings;
import org.jspecify.annotations.Nullable;

@Getter
@RequiredArgsConstructor
public enum ExperimentStatusEnum implements ExperimentOpsEnum {
    PENDING(0),
    VALIDATING(1),
    QUEUED(2),
    RUNNING(3),
    SUCCEEDED(4),
    FAILED(5),
    CANCELLED(6);

    private final int code;


    @Nullable
    public static ExperimentStatusEnum of(String name) {
        for (ExperimentStatusEnum status : ExperimentStatusEnum.values()) {
            if (Strings.CI.equals(status.name(), name)) {
                return status;
            }
        }
        return null;
    }
}
