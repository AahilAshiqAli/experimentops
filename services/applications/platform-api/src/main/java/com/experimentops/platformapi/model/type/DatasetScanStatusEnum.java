package com.experimentops.platformapi.model.type;

import com.experimentops.utils.model.enums.ExperimentOpsEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.Strings;
import org.jspecify.annotations.Nullable;

@Getter
@RequiredArgsConstructor
public enum DatasetScanStatusEnum implements ExperimentOpsEnum {
    NOT_STARTED(0),
    PENDING(1),
    IN_PROGRESS(2),
    COMPLETED(3),
    FAILED(4);

    private final int code;

    @Nullable
    public static DatasetScanStatusEnum of(String name) {
        for (DatasetScanStatusEnum status : DatasetScanStatusEnum.values()) {
            if (Strings.CI.equals(status.name(), name)) {
                return status;
            }
        }
        return null;
    }
}
