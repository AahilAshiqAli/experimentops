package com.experimentops.platformapi.model.type.converter;

import com.experimentops.platformapi.model.type.ExperimentStatusEnum;
import com.experimentops.utils.model.converter.ExperimentOpsEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ExperimentStatusEnumConverter extends ExperimentOpsEnumConverter<ExperimentStatusEnum> {
    public ExperimentStatusEnumConverter() {
        super(ExperimentStatusEnum.class);
    }
}
