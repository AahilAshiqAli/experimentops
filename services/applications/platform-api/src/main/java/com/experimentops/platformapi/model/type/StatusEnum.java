package com.experimentops.platformapi.model.type;

import com.experimentops.utils.model.enums.ExperimentOpsEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.Strings;
import org.jspecify.annotations.Nullable;

@Getter
@RequiredArgsConstructor
public enum StatusEnum implements ExperimentOpsEnum {
    PENDING(0),
    ACTIVE(1),
    INACTIVE(2);

    private final int code;

    @Nullable
    public static StatusEnum of(int code) {
        for (StatusEnum status : StatusEnum.values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        return null;
    }

    @Nullable
    public static StatusEnum of(String name) {
        for (StatusEnum status : StatusEnum.values()) {
            if (Strings.CI.equals(status.name(), name)) {
                return status;
            }
        }
        return null;
    }
}