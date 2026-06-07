package com.experimentops.platformapi.model.type;

import com.experimentops.utils.model.enums.ExperimentOpsEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.Strings;
import org.jspecify.annotations.Nullable;

@Getter
@RequiredArgsConstructor
public enum NotificationStatusEnum implements ExperimentOpsEnum {
    PENDING(0),
    SUCCESSFUL(1),
    FAILED(2);

    private final int code;

    @Nullable
    public static NotificationStatusEnum of(String name) {
        for (NotificationStatusEnum status : NotificationStatusEnum.values()) {
            if (Strings.CI.equals(status.name(), name)) {
                return status;
            }
        }
        return null;
    }
}
