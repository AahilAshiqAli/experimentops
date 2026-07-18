package com.experimentops.platformapi.validator;

import com.experimentops.common.exceptions.constant.ErrorCode;
import com.experimentops.common.exceptions.runtime.ValidationException;
import com.experimentops.experiment.type.model.v1.ExperimentTypeDefaultConfigModel;
import com.experimentops.experiment.type.model.v1.ExperimentTypeManifestModel;
import com.experimentops.experiment.type.model.v1.ExperimentTypeRequestModel;
import com.experimentops.experiment.type.model.v1.FormatStrategyModel;
import com.experimentops.experiment.type.model.v1.InputContractModel;
import com.experimentops.experiment.type.model.v1.InputManifestModel;
import com.experimentops.experiment.type.model.v1.InputRelationshipModel;
import com.experimentops.experiment.type.model.v1.OutputManifestModel;
import com.experimentops.utils.ExperimentOpsUtils;
import org.apache.commons.lang3.StringUtils;
import org.openapitools.jackson.nullable.JsonNullable;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.HashSet;
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

    private void validateFormatMappings(@Nullable List<ExperimentTypeManifestModel> formatMappings) {
        if (ExperimentOpsUtils.isEmpty(formatMappings)) {
            throw new ValidationException(ErrorCode.REQUIRED_FIELD_MISSING, "formatMappings");
        }
        formatMappings.forEach(this::validateManifestItem);
    }

    private void validateTimeWeight(@Nullable Integer timeWeight) {
        if (timeWeight == null) {
            throw new ValidationException(ErrorCode.REQUIRED_FIELD_MISSING, "timeWeight");
        }
        if (!TIME_WEIGHTS.contains(timeWeight)) {
            throw new ValidationException(ErrorCode.INVALID_INPUTS, "timeWeight");
        }
    }

    private void validateManifestItem(@Nullable ExperimentTypeManifestModel manifest) {
        if (manifest == null) {
            throw new ValidationException(ErrorCode.INVALID_INPUTS, "formatMappings item");
        }
        Set<String> inputPorts = validateInputs(manifest.getInputs());
        validateInputRelationships(manifest.getInputRelationships(), inputPorts);
        validateOutputs(manifest.getOutputs(), inputPorts);
    }

    private Set<String> validateInputs(@Nullable List<InputManifestModel> inputs) {
        if (ExperimentOpsUtils.isEmpty(inputs)) {
            throw new ValidationException(ErrorCode.REQUIRED_FIELD_MISSING, "formatMappings.inputs");
        }
        Set<String> inputPorts = new HashSet<>();
        for (InputManifestModel input : inputs) {
            validateInputManifest(input, inputPorts);
        }
        return inputPorts;
    }

    private void validateInputManifest(@Nullable InputManifestModel input, @NonNull Set<String> inputPorts) {
        if (input == null) {
            throw new ValidationException(ErrorCode.INVALID_INPUTS, "formatMappings.inputs item");
        }
        validateInputString("formatMappings.inputs.portName", input.getPortName());
        if (!inputPorts.add(input.getPortName())) {
            throw new ValidationException(ErrorCode.INVALID_INPUTS, "formatMappings.inputs.portName should be unique");
        }
        if (input.getRequired() == null) {
            throw new ValidationException(ErrorCode.REQUIRED_FIELD_MISSING, "formatMappings.inputs.required");
        }
        validateInputString("formatMappings.inputs.cardinality", input.getCardinality());
        validateInputContract(input.getContract());
    }

    private void validateInputContract(@Nullable InputContractModel contract) {
        if (contract == null) {
            throw new ValidationException(ErrorCode.REQUIRED_FIELD_MISSING, "formatMappings.inputs.contract");
        }
        if (contract.getDataKind() == null) {
            throw new ValidationException(ErrorCode.REQUIRED_FIELD_MISSING, "formatMappings.inputs.contract.dataKind");
        }
        if (ExperimentOpsUtils.isEmpty(contract.getAcceptedFormats())) {
            throw new ValidationException(ErrorCode.REQUIRED_FIELD_MISSING, "formatMappings.inputs.contract.acceptedFormats");
        }
    }

    private void validateInputRelationships(@Nullable List<InputRelationshipModel> relationships, @NonNull Set<String> inputPorts) {
        if (ExperimentOpsUtils.isEmpty(relationships)) {
            return;
        }
        if (inputPorts.size() <= 1) {
            throw new ValidationException(ErrorCode.INVALID_INPUTS, "formatMappings.inputRelationships is only applicable when inputs has more than one item");
        }
        relationships.forEach(relationship -> validateInputRelationship(relationship, inputPorts));
    }

    private void validateInputRelationship(@Nullable InputRelationshipModel relationship, @NonNull Set<String> inputPorts) {
        if (relationship == null) {
            throw new ValidationException(ErrorCode.INVALID_INPUTS, "formatMappings.inputRelationships item");
        }
        if (relationship.getType() == null) {
            throw new ValidationException(ErrorCode.REQUIRED_FIELD_MISSING, "formatMappings.inputRelationships.type");
        }
        if (relationship.getType() != InputRelationshipModel.TypeEnum.SAME_FORMAT) {
            throw new ValidationException(ErrorCode.INVALID_INPUTS, "formatMappings.inputRelationships.type");
        }
        if (ExperimentOpsUtils.isEmpty(relationship.getPorts()) || relationship.getPorts().size() < 2) {
            throw new ValidationException(ErrorCode.INVALID_INPUTS, "formatMappings.inputRelationships.ports should contain at least two ports");
        }
        relationship.getPorts().forEach(port -> validateKnownPort("formatMappings.inputRelationships.ports", port, inputPorts));
    }

    private void validateOutputs(@Nullable List<OutputManifestModel> outputs, @NonNull Set<String> inputPorts) {
        if (ExperimentOpsUtils.isEmpty(outputs)) {
            throw new ValidationException(ErrorCode.REQUIRED_FIELD_MISSING, "formatMappings.outputs");
        }
        Set<String> outputNames = new HashSet<>();
        for (OutputManifestModel output : outputs) {
            validateOutputManifest(output, inputPorts, outputNames);
        }
    }

    private void validateOutputManifest(@Nullable OutputManifestModel output, @NonNull Set<String> inputPorts, @NonNull Set<String> outputNames) {
        if (output == null) {
            throw new ValidationException(ErrorCode.INVALID_INPUTS, "formatMappings.outputs item");
        }
        validateInputString("formatMappings.outputs.name", output.getName());
        if (!outputNames.add(output.getName())) {
            throw new ValidationException(ErrorCode.INVALID_INPUTS, "formatMappings.outputs.name should be unique");
        }
        if (output.getRequired() == null) {
            throw new ValidationException(ErrorCode.REQUIRED_FIELD_MISSING, "formatMappings.outputs.required");
        }
        if (output.getDataKind() == null) {
            throw new ValidationException(ErrorCode.REQUIRED_FIELD_MISSING, "formatMappings.outputs.dataKind");
        }
        if (output.getDownStreamPolicy() == null) {
            throw new ValidationException(ErrorCode.REQUIRED_FIELD_MISSING, "formatMappings.outputs.downStreamPolicy");
        }
        validateFormatStrategy(output.getType(), inputPorts);
    }

    private void validateFormatStrategy(@Nullable FormatStrategyModel strategy, @NonNull Set<String> inputPorts) {
        if (strategy == null) {
            throw new ValidationException(ErrorCode.REQUIRED_FIELD_MISSING, "formatMappings.outputs.type");
        }
        if (strategy.getType() == null) {
            throw new ValidationException(ErrorCode.REQUIRED_FIELD_MISSING, "formatMappings.outputs.type.type");
        }
        if (strategy.getType() == FormatStrategyModel.TypeEnum.FIXED) {
            if (getNullableFormat(strategy.getFormat()) == null) {
                throw new ValidationException(ErrorCode.REQUIRED_FIELD_MISSING, "formatMappings.outputs.type.format");
            }
            return;
        }
        String sourceInputPort = getNullableString(strategy.getSourceInputPort());
        if (StringUtils.isBlank(sourceInputPort)) {
            throw new ValidationException(ErrorCode.REQUIRED_FIELD_MISSING, "formatMappings.outputs.type.sourceInputPort");
        }
        validateKnownPort("formatMappings.outputs.type.sourceInputPort", sourceInputPort, inputPorts);
    }

    private void validateKnownPort(@NonNull String field, @Nullable String port, @NonNull Set<String> inputPorts) {
        validateInputString(field, port);
        if (!inputPorts.contains(port)) {
            throw new ValidationException(ErrorCode.INVALID_INPUTS, field + " should reference an existing input port");
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

    private FormatStrategyModel.FormatEnum getNullableFormat(JsonNullable<FormatStrategyModel.FormatEnum> value) {
        if (value == null || !value.isPresent()) {
            return null;
        }
        return value.get();
    }

}
