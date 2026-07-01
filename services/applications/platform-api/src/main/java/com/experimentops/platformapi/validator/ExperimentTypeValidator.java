package com.experimentops.platformapi.validator;

import com.experimentops.common.exceptions.constant.ErrorCode;
import com.experimentops.common.exceptions.runtime.ValidationException;
import com.experimentops.experiment.type.model.v1.ExperimentTypeDefaultConfigModel;
import com.experimentops.experiment.type.model.v1.ExperimentTypeRequestModel;
import org.openapitools.jackson.nullable.JsonNullable;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ExperimentTypeValidator extends GenericValidator {

    public void validateExperimentTypeRequestModel(@NonNull ExperimentTypeRequestModel requestModel) {
        validateInputString("name", requestModel.getName());
        validateDefaultConfig(requestModel.getDefaultConfig());
    }

    private void validateDefaultConfig(List<ExperimentTypeDefaultConfigModel> defaultConfig) {
        if (defaultConfig == null || defaultConfig.isEmpty()) {
            throw new ValidationException(ErrorCode.REQUIRED_FIELD_MISSING, "defaultConfig");
        }
        defaultConfig.forEach(this::validateDefaultConfigItem);
    }

    private void validateDefaultConfigItem(ExperimentTypeDefaultConfigModel configItem) {
        if (configItem == null) {
            throw new ValidationException(ErrorCode.INVALID_INPUTS, "defaultConfig item");
        }
        validateInputString("defaultConfig.name", configItem.getName());
        ExperimentTypeDefaultConfigModel.DatatypeEnum datatype = configItem.getDatatype();
        if (datatype == null) {
            throw new ValidationException(ErrorCode.INVALID_INPUTS, "defaultConfig." + configItem.getName() + ".datatype should be a supported datatype");
        }
        validateDefaultValue(configItem.getName(), datatype, configItem.getDefaultValue());
    }

    private void validateDefaultValue(String name, ExperimentTypeDefaultConfigModel.DatatypeEnum datatype, JsonNullable<Object> defaultValue) {
        if (defaultValue == null || !defaultValue.isPresent()) {
            return;
        }
        Object value = defaultValue.get();
        if (value == null) {
            return;
        }
        boolean valid = switch (datatype.getValue()) {
            case "boolean" -> value instanceof Boolean;
            case "string" -> value instanceof String;
            case "number" -> value instanceof Number;
            case "list" -> value instanceof List<?>;
            default -> false;
        };
        if (!valid) {
            throw new ValidationException(ErrorCode.INVALID_INPUTS, "defaultConfig." + name + ".defaultValue should match datatype " + datatype.getValue());
        }
    }

}
