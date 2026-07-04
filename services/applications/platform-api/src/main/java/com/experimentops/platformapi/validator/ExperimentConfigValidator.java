package com.experimentops.platformapi.validator;

import com.experimentops.common.exceptions.constant.ErrorCode;
import com.experimentops.common.exceptions.runtime.ValidationException;
import com.experimentops.experiment.model.v1.ExperimentConfigRequestModel;
import com.experimentops.experiment.model.v1.ExperimentConfigStatusChangeRequestModel;
import com.experimentops.platformapi.model.entity.ExperimentType;
import com.experimentops.platformapi.model.entity.ExperimentTypeDefaultConfig;
import com.experimentops.platformapi.model.type.StatusEnum;
import com.experimentops.utils.ExperimentOpsUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ExperimentConfigValidator extends GenericValidator {

    public void validateExperimentConfigRequestModel(@NonNull ExperimentConfigRequestModel requestModel) {
        validateInputString("name", requestModel.getName());
        if (ExperimentOpsUtils.isEmpty(requestModel.getConfig())) {
            throw new ValidationException(ErrorCode.REQUIRED_FIELD_MISSING, "config");
        }
    }

    public StatusEnum validateExperimentConfigStatusChangeRequestModel(@NonNull ExperimentConfigStatusChangeRequestModel requestModel) {
        StatusEnum status = StatusEnum.of(requestModel.getStatus());
        if (status == null) {
            throw new ValidationException(ErrorCode.INVALID_INPUTS, "status");
        }
        return status;
    }

    public void validateExperimentConfigMatchesExperimentTypeConfig(@NonNull ExperimentType experimentType, @NonNull Map<String, Object> config) {
        Map<String, ExperimentTypeDefaultConfig.Datatype> expectedConfig = getStringDatatypeMap(experimentType);

        for (String expectedConfigKey : expectedConfig.keySet()) {
            if (!config.containsKey(expectedConfigKey)) {
                throw new ValidationException(ErrorCode.REQUIRED_FIELD_MISSING, "config." + expectedConfigKey);
            }
        }

        for (Map.Entry<String, Object> configEntry : config.entrySet()) {
            ExperimentTypeDefaultConfig.Datatype expectedDatatype = expectedConfig.get(configEntry.getKey());
            validateConfigValueDatatype(configEntry.getKey(), configEntry.getValue(), expectedDatatype);
        }
    }

    private @NonNull Map<String, ExperimentTypeDefaultConfig.Datatype> getStringDatatypeMap(@NonNull ExperimentType experimentType) {
        List<ExperimentTypeDefaultConfig> defaultConfig = experimentType.getDefaultConfig();
        if (ExperimentOpsUtils.isEmpty(defaultConfig)) {
            throw new ValidationException(ErrorCode.INVALID_INPUTS, "experimentType.defaultConfig");
        }

        Map<String, ExperimentTypeDefaultConfig.Datatype> expectedConfig = new HashMap<>();
        for (ExperimentTypeDefaultConfig defaultConfigItem : defaultConfig) {
            if (defaultConfigItem == null || defaultConfigItem.getName() == null || defaultConfigItem.getDatatype() == null) {
                throw new ValidationException(ErrorCode.INVALID_INPUTS, "experimentType.defaultConfig");
            }
            if (expectedConfig.put(defaultConfigItem.getName(), defaultConfigItem.getDatatype()) != null) {
                throw new ValidationException(ErrorCode.INVALID_INPUTS, "experimentType.defaultConfig." + defaultConfigItem.getName());
            }
        }
        return expectedConfig;
    }

    private void validateConfigValueDatatype(@Nullable String name, @Nullable Object value, ExperimentTypeDefaultConfig.@NonNull Datatype datatype) {
        if (value == null) {
            throw new ValidationException(ErrorCode.INVALID_INPUTS, "config." + name + " should match datatype " + datatype.getValue());
        }

        boolean isValid = switch (datatype) {
            case BOOLEAN -> value instanceof Boolean;
            case STRING -> value instanceof String;
            case NUMBER -> value instanceof Number;
            case LIST -> value instanceof List<?>;
        };

        if (!isValid) {
            throw new ValidationException(ErrorCode.INVALID_INPUTS, "config." + name + " should match datatype " + datatype.getValue());
        }
    }
}
