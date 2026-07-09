package com.experimentops.platformapi.validator;

import com.experimentops.common.exceptions.constant.ErrorCode;
import com.experimentops.common.exceptions.runtime.ValidationException;
import com.experimentops.experiment.type.model.v1.ExperimentTypeDefaultConfigModel;
import com.experimentops.experiment.type.model.v1.ExperimentTypeRequestModel;
import org.openapitools.jackson.nullable.JsonNullable;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

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
        String regex = getNullableString(configItem.getRegex());
        validateRegex(configItem.getName(), datatype, regex);
        validateDefaultValue(configItem.getName(), datatype, configItem.getDefaultValue(), regex);
    }

    private void validateDefaultValue(String name, ExperimentTypeDefaultConfigModel.DatatypeEnum datatype, JsonNullable<Object> defaultValue, String regex) {
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
        validateValueMatchesRegex("defaultConfig." + name + ".defaultValue", value, datatype, regex);
    }

    private void validateRegex(String name, ExperimentTypeDefaultConfigModel.DatatypeEnum datatype, String regex) {
        if (regex == null || regex.isBlank()) {
            return;
        }
        if (datatype != ExperimentTypeDefaultConfigModel.DatatypeEnum.STRING) {
            throw new ValidationException(ErrorCode.INVALID_INPUTS, "defaultConfig." + name + ".regex is only supported for string datatype");
        }
        try {
            Pattern.compile(regex);
        } catch (PatternSyntaxException e) {
            throw new ValidationException(ErrorCode.INVALID_INPUTS, "defaultConfig." + name + ".regex is invalid");
        }
    }

    private void validateValueMatchesRegex(String field, Object value, ExperimentTypeDefaultConfigModel.DatatypeEnum datatype, String regex) {
        if (regex == null || regex.isBlank() || value == null) {
            return;
        }
        if (datatype == ExperimentTypeDefaultConfigModel.DatatypeEnum.STRING && !Pattern.matches(regex, value.toString())) {
            throw new ValidationException(ErrorCode.INVALID_INPUTS, field + " should match regex");
        }
    }

    private String getNullableString(JsonNullable<String> value) {
        if (value == null || !value.isPresent()) {
            return null;
        }
        return value.get();
    }

}
