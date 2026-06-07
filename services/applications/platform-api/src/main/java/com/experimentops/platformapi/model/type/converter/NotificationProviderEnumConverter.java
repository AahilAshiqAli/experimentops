package com.experimentops.platformapi.model.type.converter;

import com.experimentops.platformapi.model.type.NotificationProviderEnum;
import com.experimentops.utils.model.converter.ExperimentOpsEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class NotificationProviderEnumConverter extends ExperimentOpsEnumConverter<NotificationProviderEnum> {
    public NotificationProviderEnumConverter() {
        super(NotificationProviderEnum.class);
    }
}
