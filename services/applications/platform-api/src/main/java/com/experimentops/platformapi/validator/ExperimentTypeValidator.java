package com.experimentops.platformapi.validator;

import com.experimentops.common.exceptions.constant.ErrorCode;
import com.experimentops.common.exceptions.runtime.ValidationException;
import com.experimentops.experiment.type.model.v1.ExperimentTypeDefaultConfigModel;
import com.experimentops.experiment.type.model.v1.ExperimentTypeFormatMappingModel;
import com.experimentops.experiment.type.model.v1.ExperimentTypeRequestModel;
import com.experimentops.utils.ExperimentOpsUtils;
import org.apache.commons.lang3.StringUtils;
import org.openapitools.jackson.nullable.JsonNullable;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Component
public class ExperimentTypeValidator extends GenericValidator {
    private static final Set<Integer> TIME_WEIGHTS = Set.of(1, 2, 3, 5, 8, 13, 20);

    public void validateExperimentTypeRequestModel(@NonNull ExperimentTypeRequestModel requestModel) {
        validateInputString("name", requestModel.getName());
        validateDefaultConfig(requestModel.getDefaultConfig());
        validateFormatMappings(requestModel.getFormatMappings());
        validateTimeWeight(requestModel.getTimeWeight());
    }

    private void validateDefaultConfig(@Nullable List<ExperimentTypeDefaultConfigModel> defaultConfig) {
        if (ExperimentOpsUtils.isEmpty(defaultConfig)) {
            throw new ValidationException(ErrorCode.REQUIRED_FIELD_MISSING, "defaultConfig");
        }
        defaultConfig.forEach(this::validateDefaultConfigItem);
    }

    private void validateFormatMappings(@Nullable List<ExperimentTypeFormatMappingModel> formatMappings) {
        if (ExperimentOpsUtils.isEmpty(formatMappings)) {
            throw new ValidationException(ErrorCode.REQUIRED_FIELD_MISSING, "formatMappings");
        }
        formatMappings.forEach(this::validateFormatMappingItem);
    }

    private void validateTimeWeight(@Nullable Integer timeWeight) {
        if (timeWeight == null) {
            throw new ValidationException(ErrorCode.REQUIRED_FIELD_MISSING, "timeWeight");
        }
        if (!TIME_WEIGHTS.contains(timeWeight)) {
            throw new ValidationException(ErrorCode.INVALID_INPUTS, "timeWeight");
        }
    }

    private void validateFormatMappingItem(@Nullable ExperimentTypeFormatMappingModel formatMapping) {
        if (formatMapping == null) {
            throw new ValidationException(ErrorCode.INVALID_INPUTS, "formatMappings item");
        }
        if (formatMapping.getInputFormat() == null) {
            throw new ValidationException(ErrorCode.INVALID_INPUTS, "formatMappings.inputFormat");
        }
        if (formatMapping.getOutputFormat() == null) {
            throw new ValidationException(ErrorCode.INVALID_INPUTS, "formatMappings.outputFormat");
        }
    }

    private void validateDefaultConfigItem(@Nullable ExperimentTypeDefaultConfigModel configItem) {
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

    private void validateDefaultValue(@NonNull String name, ExperimentTypeDefaultConfigModel.DatatypeEnum datatype, @Nullable JsonNullable<Object> defaultValue, @Nullable String regex) {
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

    private void validateRegex(@NonNull String name, ExperimentTypeDefaultConfigModel.DatatypeEnum datatype, @Nullable String regex) {
        if (StringUtils.isBlank(regex)) {
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

    private void validateValueMatchesRegex(@NonNull String field, @Nullable Object value, ExperimentTypeDefaultConfigModel.DatatypeEnum datatype, @Nullable String regex) {
        if (StringUtils.isBlank(regex) || value == null) {
            return;
        }
        if (datatype == ExperimentTypeDefaultConfigModel.DatatypeEnum.STRING && !Pattern.matches(regex, value.toString())) {
            throw new ValidationException(ErrorCode.INVALID_INPUTS, field + " should match regex");
        }
    }

    @Nullable
    private String getNullableString(@Nullable JsonNullable<String> value) {
        if (value == null || !value.isPresent()) {
            return null;
        }
        return value.get();
    }

}
