package com.experimentops.platformapi.model.type.converter;

import com.experimentops.platformapi.model.type.DatasetScanStatusEnum;
import com.experimentops.utils.model.converter.ExperimentOpsEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class DatasetScanStatusEnumConverter extends ExperimentOpsEnumConverter<DatasetScanStatusEnum> {
    public DatasetScanStatusEnumConverter() {
        super(DatasetScanStatusEnum.class);
    }
}
