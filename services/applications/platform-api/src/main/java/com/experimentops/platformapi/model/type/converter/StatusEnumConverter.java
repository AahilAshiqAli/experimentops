package com.experimentops.platformapi.model.type.converter;

import com.experimentops.platformapi.model.type.StatusEnum;
import com.experimentops.utils.model.converter.ExperimentOpsEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class StatusEnumConverter extends ExperimentOpsEnumConverter<StatusEnum> {
    public StatusEnumConverter() {
        super(StatusEnum.class);
    }
}