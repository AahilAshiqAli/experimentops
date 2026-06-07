package com.experimentops.platformapi.model.type;

import com.experimentops.utils.model.enums.ExperimentOpsEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.Strings;
import org.jspecify.annotations.Nullable;

@Getter
@RequiredArgsConstructor
public enum NotificationProviderEnum implements ExperimentOpsEnum {
    EMAIL(0);

    private final int code;

    @Nullable
    public static NotificationProviderEnum of(String name) {
        for (NotificationProviderEnum provider : NotificationProviderEnum.values()) {
            if (Strings.CI.equals(provider.name(), name)) {
                return provider;
            }
        }
        return null;
    }
}
