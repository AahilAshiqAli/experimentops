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
import java.util.regex.Pattern;

@Component
public class ExperimentConfigValidator extends GenericValidator {

    public void validateExperimentConfigRequestModel(@NonNull ExperimentConfigRequestModel requestModel) {
        validateInputString("name", requestModel.getName());
        validateInputString("experimentType", requestModel.getExperimentType());
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
        Map<String, ExperimentTypeDefaultConfig> expectedConfig = getDefaultConfigByName(experimentType);

        for (String expectedConfigKey : expectedConfig.keySet()) {
            if (!config.containsKey(expectedConfigKey)) {
                throw new ValidationException(ErrorCode.REQUIRED_FIELD_MISSING, "config." + expectedConfigKey);
            }
        }

        for (Map.Entry<String, Object> configEntry : config.entrySet()) {
            ExperimentTypeDefaultConfig expectedConfigItem = expectedConfig.get(configEntry.getKey());
            if (expectedConfigItem == null) {
                throw new ValidationException(ErrorCode.INVALID_INPUTS, "config." + configEntry.getKey());
            }
            validateConfigValue(configEntry.getKey(), configEntry.getValue(), expectedConfigItem);
        }
    }

    private @NonNull Map<String, ExperimentTypeDefaultConfig> getDefaultConfigByName(@NonNull ExperimentType experimentType) {
        List<ExperimentTypeDefaultConfig> defaultConfig = experimentType.getDefaultConfig();
        if (ExperimentOpsUtils.isEmpty(defaultConfig)) {
            throw new ValidationException(ErrorCode.INVALID_INPUTS, "experimentType.defaultConfig");
        }

        Map<String, ExperimentTypeDefaultConfig> expectedConfig = new HashMap<>();
        for (ExperimentTypeDefaultConfig defaultConfigItem : defaultConfig) {
            if (defaultConfigItem == null || defaultConfigItem.getName() == null || defaultConfigItem.getDatatype() == null) {
                throw new ValidationException(ErrorCode.INVALID_INPUTS, "experimentType.defaultConfig");
            }
            if (expectedConfig.put(defaultConfigItem.getName(), defaultConfigItem) != null) {
                throw new ValidationException(ErrorCode.INVALID_INPUTS, "experimentType.defaultConfig." + defaultConfigItem.getName());
            }
        }
        return expectedConfig;
    }

    private void validateConfigValue(@Nullable String name, @Nullable Object value, @NonNull ExperimentTypeDefaultConfig defaultConfigItem) {
        ExperimentTypeDefaultConfig.Datatype datatype = defaultConfigItem.getDatatype();
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
        validateConfigValueRegex(name, value, defaultConfigItem);
    }

    private void validateConfigValueRegex(@Nullable String name, @NonNull Object value, @NonNull ExperimentTypeDefaultConfig defaultConfigItem) {
        String regex = defaultConfigItem.getRegex();
        if (regex == null || regex.isBlank()) {
            return;
        }
        if (defaultConfigItem.getDatatype() == ExperimentTypeDefaultConfig.Datatype.STRING && !Pattern.matches(regex, value.toString())) {
            throw new ValidationException(ErrorCode.INVALID_INPUTS, "config." + name + " should match regex");
        }
    }
}
