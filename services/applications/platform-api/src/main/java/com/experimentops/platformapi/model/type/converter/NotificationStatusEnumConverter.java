package com.experimentops.platformapi.model.type.converter;

import com.experimentops.platformapi.model.type.NotificationStatusEnum;
import com.experimentops.utils.model.converter.ExperimentOpsEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class NotificationStatusEnumConverter extends ExperimentOpsEnumConverter<NotificationStatusEnum> {
    public NotificationStatusEnumConverter() {
        super(NotificationStatusEnum.class);
    }
}
