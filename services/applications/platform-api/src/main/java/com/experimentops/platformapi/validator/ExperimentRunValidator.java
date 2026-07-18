package com.experimentops.platformapi.validator;

import com.experimentops.common.exceptions.constant.ErrorCode;
import com.experimentops.common.exceptions.runtime.ValidationException;
import com.experimentops.experiment.run.model.v1.ExperimentRunExecutionModeModel;
import com.experimentops.experiment.run.model.v1.ExperimentRunInputModel;
import com.experimentops.experiment.run.model.v1.ExperimentRunRequestModel;
import com.experimentops.experiment.run.model.v1.ExperimentRunStatusRequestModel;
import com.experimentops.platformapi.model.ExperimentRunConfigContext;
import com.experimentops.platformapi.model.ExperimentRunResolvedPlan;
import com.experimentops.platformapi.model.entity.DatasetVersion;
import com.experimentops.platformapi.model.entity.DownStreamPolicyEnum;
import com.experimentops.platformapi.model.entity.ExperimentTypeManifest;
import com.experimentops.platformapi.model.entity.FormatStrategy;
import com.experimentops.platformapi.model.entity.FormatStrategyTypeEnum;
import com.experimentops.platformapi.model.entity.InputManifest;
import com.experimentops.platformapi.model.entity.InputRelationship;
import com.experimentops.platformapi.model.entity.InputRelationshipTypeEnum;
import com.experimentops.platformapi.model.entity.OutputManifest;
import com.experimentops.platformapi.model.type.DatasetFileFormatEnum;
import com.experimentops.platformapi.model.type.DatasetScanStatusEnum;
import com.experimentops.platformapi.model.type.ExperimentStatusEnum;
import com.experimentops.utils.ExperimentOpsUtils;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ExperimentRunValidator extends GenericValidator {

    public void validateExperimentRunRequestModel(@NonNull ExperimentRunRequestModel requestModel, @NonNull Boolean validateName) {
        if (validateName)
            validateInputString("name", requestModel.getName());
        validateExecutionMode(requestModel.getExecutionMode());
    }

    public void validateExperimentRunRequestModel(@NonNull String experimentUuid, @NonNull ExperimentRunRequestModel requestModel, @NonNull Boolean validateName) {
        validateInputString("experimentUuid", experimentUuid);
        validateExperimentRunRequestModel(requestModel, validateName);
    }

    @NonNull
    public List<String> collectDatasetVersionUuids(@NonNull List<ExperimentRunExecutionModeModel> executionMode) {
        return executionMode.stream()
                .flatMap(executionModeItem -> executionModeItem.getInputs().stream())
                .filter(input -> input.getInputType() == ExperimentRunInputModel.InputTypeEnum.DATASET)
                .map(ExperimentRunInputModel::getFile)
                .distinct()
                .toList();
    }

    @NonNull
    public List<String> validateExperimentRunStatusRequestModel(@NonNull ExperimentRunStatusRequestModel requestModel) {
        List<String> experimentRunUuids = requestModel.getExperimentRunUuids();
        if (ExperimentOpsUtils.isEmpty(experimentRunUuids)) {
            throw new ValidationException(ErrorCode.REQUIRED_FIELD_MISSING, "experimentRunUuids");
        }

        LinkedHashSet<String> uniqueExperimentRunUuids = new LinkedHashSet<>();
        for (String experimentRunUuid : experimentRunUuids) {
            validateInputString("experimentRunUuids", experimentRunUuid);
            uniqueExperimentRunUuids.add(experimentRunUuid);
        }

        return List.copyOf(uniqueExperimentRunUuids);
    }

    @Nullable
    public List<ExperimentStatusEnum> getExperimentStatuses(@Nullable String status) {
        if (StringUtils.isBlank(status)) {
            return null;
        }

        LinkedHashSet<ExperimentStatusEnum> experimentStatuses = new LinkedHashSet<>();
        for (String statusItem : status.split(",")) {
            if (StringUtils.isBlank(statusItem)) {
                continue;
            }

            ExperimentStatusEnum experimentStatus = ExperimentStatusEnum.of(statusItem.trim());
            if (experimentStatus == null) {
                throw new ValidationException(ErrorCode.INVALID_INPUTS, "status");
            }
            experimentStatuses.add(experimentStatus);
        }

        if (ExperimentOpsUtils.isEmpty(experimentStatuses)) {
            return null;
        }
        return List.copyOf(experimentStatuses);
    }

    @NonNull
    public ExperimentRunResolvedPlan validateAndResolveExecutionPlan(
            @NonNull List<ExperimentRunExecutionModeModel> executionMode,
            @NonNull Map<String, ExperimentRunConfigContext> experimentConfigsByUuid,
            @NonNull Map<String, DatasetVersion> datasetVersionsByUuid) {

        List<ExperimentRunExecutionModeModel> sortedExecutionMode = executionMode.stream()
                .sorted(Comparator.comparing(ExperimentRunExecutionModeModel::getStepCount))
                .toList();

        Set<OutputReference> downstreamOutputReferences = collectDownstreamOutputReferences(sortedExecutionMode);
        Map<Integer, Map<String, ExperimentRunResolvedPlan.Output>> outputsByStep = new HashMap<>();
        List<ExperimentRunResolvedPlan.Step> resolvedSteps = new ArrayList<>();

        for (ExperimentRunExecutionModeModel executionModeItem : sortedExecutionMode) {
            ExperimentRunConfigContext experimentConfig = experimentConfigsByUuid.get(executionModeItem.getExperimentConfigUuid());
            if (experimentConfig == null) {
                throw new ValidationException(ErrorCode.INVALID_INPUTS, "executionMode.experimentConfigUuid");
            }

            ExperimentRunResolvedPlan.Step resolvedStep = resolveStep(
                    executionModeItem,
                    experimentConfig,
                    datasetVersionsByUuid,
                    outputsByStep,
                    downstreamOutputReferences
            );
            outputsByStep.put(
                    resolvedStep.getStepCount(),
                    resolvedStep.getOutputs().stream()
                            .collect(HashMap::new, (map, output) -> map.put(output.getName(), output), HashMap::putAll)
            );
            resolvedSteps.add(resolvedStep);
        }

        return ExperimentRunResolvedPlan.builder()
                .steps(resolvedSteps)
                .build();
    }

    private ExperimentRunResolvedPlan.Step resolveStep(
            @NonNull ExperimentRunExecutionModeModel executionModeItem,
            @NonNull ExperimentRunConfigContext experimentConfig,
            @NonNull Map<String, DatasetVersion> datasetVersionsByUuid,
            @NonNull Map<Integer, Map<String, ExperimentRunResolvedPlan.Output>> outputsByStep,
            @NonNull Set<OutputReference> downstreamOutputReferences) {

        List<ExperimentTypeManifest> manifests = experimentConfig.getFormatMappings();
        if (ExperimentOpsUtils.isEmpty(manifests)) {
            throw new ValidationException(ErrorCode.INVALID_INPUTS, "experimentType.formatMappings");
        }

        List<ExperimentRunResolvedPlan.Step> matches = new ArrayList<>();
        ValidationException lastFailure = null;
        for (ExperimentTypeManifest manifest : manifests) {
            try {
                matches.add(resolveStepAgainstManifest(
                        executionModeItem,
                        experimentConfig,
                        manifest,
                        datasetVersionsByUuid,
                        outputsByStep,
                        downstreamOutputReferences
                ));
            } catch (ValidationException e) {
                lastFailure = e;
            }
        }

        if (matches.size() > 1) {
            throw new ValidationException(
                    ErrorCode.INVALID_INPUTS,
                    "Step " + executionModeItem.getStepCount() + " matches more than one experiment type manifest"
            );
        }

        if (matches.isEmpty()) {
            if (lastFailure != null) {
                throw lastFailure;
            }
            throw new ValidationException(ErrorCode.INVALID_INPUTS, "experimentType.formatMappings");
        }

        return matches.getFirst();
    }

    private ExperimentRunResolvedPlan.Step resolveStepAgainstManifest(
            @NonNull ExperimentRunExecutionModeModel executionModeItem,
            @NonNull ExperimentRunConfigContext experimentConfig,
            @NonNull ExperimentTypeManifest manifest,
            @NonNull Map<String, DatasetVersion> datasetVersionsByUuid,
            @NonNull Map<Integer, Map<String, ExperimentRunResolvedPlan.Output>> outputsByStep,
            @NonNull Set<OutputReference> downstreamOutputReferences) {

        Map<String, InputManifest> manifestInputsByPort = manifestInputsByPort(manifest);
        List<ExperimentRunResolvedPlan.Input> resolvedInputs = resolveInputs(
                executionModeItem,
                manifestInputsByPort,
                datasetVersionsByUuid,
                outputsByStep
        );
        Map<String, ExperimentRunResolvedPlan.Input> resolvedInputsByPort = resolvedInputs.stream()
                .collect(HashMap::new, (map, input) -> map.put(input.getPortName(), input), HashMap::putAll);

        validateRequiredInputs(manifestInputsByPort, resolvedInputsByPort, executionModeItem.getStepCount());
        validateInputRelationships(manifest.getInputRelationships(), resolvedInputsByPort, executionModeItem.getStepCount());

        List<ExperimentRunResolvedPlan.Output> resolvedOutputs = resolveOutputs(
                manifest.getOutputs(),
                resolvedInputsByPort,
                executionModeItem.getStepCount(),
                downstreamOutputReferences
        );

        return ExperimentRunResolvedPlan.Step.builder()
                .stepCount(executionModeItem.getStepCount())
                .experimentConfigUuid(executionModeItem.getExperimentConfigUuid())
                .experimentType(experimentConfig.getExperimentType())
                .experimentConfigJson(experimentConfig.getExperimentConfigJson())
                .timeWeight(experimentConfig.getTimeWeight())
                .inputs(resolvedInputs)
                .outputs(resolvedOutputs)
                .build();
    }

    private Map<String, InputManifest> manifestInputsByPort(@NonNull ExperimentTypeManifest manifest) {
        if (ExperimentOpsUtils.isEmpty(manifest.getInputs())) {
            throw new ValidationException(ErrorCode.INVALID_INPUTS, "experimentType.formatMappings.inputs");
        }

        Map<String, InputManifest> inputsByPort = new HashMap<>();
        for (InputManifest input : manifest.getInputs()) {
            if (input == null || StringUtils.isBlank(input.getPortName())) {
                throw new ValidationException(ErrorCode.INVALID_INPUTS, "experimentType.formatMappings.inputs");
            }
            inputsByPort.put(input.getPortName(), input);
        }
        return inputsByPort;
    }

    private List<ExperimentRunResolvedPlan.Input> resolveInputs(
            @NonNull ExperimentRunExecutionModeModel executionModeItem,
            @NonNull Map<String, InputManifest> manifestInputsByPort,
            @NonNull Map<String, DatasetVersion> datasetVersionsByUuid,
            @NonNull Map<Integer, Map<String, ExperimentRunResolvedPlan.Output>> outputsByStep) {

        List<ExperimentRunResolvedPlan.Input> resolvedInputs = new ArrayList<>();
        for (ExperimentRunInputModel input : executionModeItem.getInputs()) {
            InputManifest inputManifest = manifestInputsByPort.get(input.getPortName());
            if (inputManifest == null) {
                throw new ValidationException(
                        ErrorCode.INVALID_INPUTS,
                        "Step " + executionModeItem.getStepCount() + " input port " + input.getPortName()
                                + " is not defined by experiment type"
                );
            }

            resolvedInputs.add(resolveInput(
                    executionModeItem.getStepCount(),
                    input,
                    inputManifest,
                    datasetVersionsByUuid,
                    outputsByStep
            ));
        }
        return resolvedInputs;
    }

    private ExperimentRunResolvedPlan.Input resolveInput(
            @NonNull Integer stepCount,
            @NonNull ExperimentRunInputModel input,
            @NonNull InputManifest inputManifest,
            @NonNull Map<String, DatasetVersion> datasetVersionsByUuid,
            @NonNull Map<Integer, Map<String, ExperimentRunResolvedPlan.Output>> outputsByStep) {

        if (input.getInputType() == ExperimentRunInputModel.InputTypeEnum.DATASET) {
            return resolveDatasetInput(stepCount, input, inputManifest, datasetVersionsByUuid);
        }
        return resolveArtifactInput(stepCount, input, inputManifest, outputsByStep);
    }

    private ExperimentRunResolvedPlan.Input resolveDatasetInput(
            @NonNull Integer stepCount,
            @NonNull ExperimentRunInputModel input,
            @NonNull InputManifest inputManifest,
            @NonNull Map<String, DatasetVersion> datasetVersionsByUuid) {

        DatasetVersion datasetVersion = datasetVersionsByUuid.get(input.getFile());
        if (datasetVersion == null) {
            throw new ValidationException(ErrorCode.INVALID_INPUTS, "Invalid datasetVersionUuid: " + input.getFile());
        }
        if (datasetVersion.getScanStatus() != DatasetScanStatusEnum.COMPLETED) {
            throw new ValidationException(ErrorCode.INVALID_INPUTS, "datasetVersion.scanStatus must be COMPLETED");
        }

        DatasetFileFormatEnum datasetFormat = parseFormat(datasetVersion.getFormat(), "datasetVersion.format");
        validateAcceptedFormat(stepCount, input.getPortName(), datasetFormat, inputManifest);

        return ExperimentRunResolvedPlan.Input.builder()
                .portName(input.getPortName())
                .inputType(input.getInputType().getValue())
                .dataKind(normalizeDataKind(inputManifest.getContract().getDataKind()))
                .format(datasetFormat)
                .datasetVersionUuid(datasetVersion.getUuid())
                .datasetUri(datasetVersion.getStorageUri())
                .build();
    }

    private ExperimentRunResolvedPlan.Input resolveArtifactInput(
            @NonNull Integer stepCount,
            @NonNull ExperimentRunInputModel input,
            @NonNull InputManifest inputManifest,
            @NonNull Map<Integer, Map<String, ExperimentRunResolvedPlan.Output>> outputsByStep) {

        Integer sourceStepCount = getNullableInteger(input.getSourceStepCount());
        Map<String, ExperimentRunResolvedPlan.Output> sourceOutputs = outputsByStep.get(sourceStepCount);
        if (sourceOutputs == null) {
            throw new ValidationException(ErrorCode.INVALID_INPUTS, "Step " + stepCount + " sourceStepCount does not reference a previous step");
        }

        ExperimentRunResolvedPlan.Output sourceOutput = sourceOutputs.get(input.getFile());
        if (sourceOutput == null) {
            throw new ValidationException(
                    ErrorCode.INVALID_INPUTS,
                    "Step " + stepCount + " source step " + sourceStepCount + " does not produce artifact " + input.getFile()
            );
        }
        if (sourceOutput.getDownStreamPolicy() != DownStreamPolicyEnum.CONNECTABLE) {
            throw new ValidationException(
                    ErrorCode.INVALID_INPUTS,
                    "Step " + stepCount + " can only use CONNECTABLE artifact " + input.getFile()
            );
        }

        validateAcceptedFormat(stepCount, input.getPortName(), sourceOutput.getFormat(), inputManifest);
        validateArtifactDataKind(stepCount, input.getPortName(), sourceOutput, inputManifest);

        return ExperimentRunResolvedPlan.Input.builder()
                .portName(input.getPortName())
                .inputType(input.getInputType().getValue())
                .dataKind(sourceOutput.getDataKind())
                .format(sourceOutput.getFormat())
                .sourceStepCount(sourceStepCount)
                .artifactName(sourceOutput.getName())
                .build();
    }

    private void validateAcceptedFormat(
            @NonNull Integer stepCount,
            @NonNull String portName,
            @NonNull DatasetFileFormatEnum format,
            @NonNull InputManifest inputManifest) {

        if (inputManifest.getContract() == null || ExperimentOpsUtils.isEmpty(inputManifest.getContract().getAcceptedFormats())) {
            throw new ValidationException(ErrorCode.INVALID_INPUTS, "experimentType.formatMappings.inputs.contract.acceptedFormats");
        }
        if (!inputManifest.getContract().getAcceptedFormats().contains(format)) {
            throw new ValidationException(
                    ErrorCode.INVALID_INPUTS,
                    "Step " + stepCount + " input port " + portName + " does not accept format " + format.name()
            );
        }
    }

    private void validateArtifactDataKind(
            @NonNull Integer stepCount,
            @NonNull String portName,
            ExperimentRunResolvedPlan.Output sourceOutput,
            @NonNull InputManifest inputManifest) {

        String expectedDataKind = normalizeDataKind(inputManifest.getContract() == null ? null : inputManifest.getContract().getDataKind());
        if (StringUtils.isBlank(expectedDataKind) || expectedDataKind.equals(normalizeDataKind(sourceOutput.getDataKind()))) {
            return;
        }
        throw new ValidationException(
                ErrorCode.INVALID_INPUTS,
                "Step " + stepCount + " input port " + portName + " expects dataKind " + expectedDataKind
                        + " but artifact " + sourceOutput.getName() + " is " + sourceOutput.getDataKind()
        );
    }

    private void validateRequiredInputs(
            @NonNull Map<String, InputManifest> manifestInputsByPort,
            @NonNull Map<String, ExperimentRunResolvedPlan.Input> resolvedInputsByPort,
            @NonNull Integer stepCount) {

        manifestInputsByPort.values()
                .stream()
                .filter(input -> Boolean.TRUE.equals(input.getRequired()))
                .filter(input -> !resolvedInputsByPort.containsKey(input.getPortName()))
                .findFirst()
                .ifPresent(input -> {
                    throw new ValidationException(
                            ErrorCode.REQUIRED_FIELD_MISSING,
                            "Step " + stepCount + " required input port " + input.getPortName()
                    );
                });
    }

    private void validateInputRelationships(
            @Nullable List<InputRelationship> inputRelationships,
            @NonNull Map<String, ExperimentRunResolvedPlan.Input> resolvedInputsByPort,
            @NonNull Integer stepCount) {

        if (ExperimentOpsUtils.isEmpty(inputRelationships)) {
            return;
        }

        for (InputRelationship relationship : inputRelationships) {
            if (relationship == null || relationship.getType() != InputRelationshipTypeEnum.SAME_FORMAT) {
                continue;
            }

            List<ExperimentRunResolvedPlan.Input> suppliedRelationshipInputs = relationship.getPorts()
                    .stream()
                    .map(resolvedInputsByPort::get)
                    .filter(input -> input != null)
                    .toList();

            if (suppliedRelationshipInputs.size() < 2) {
                continue;
            }

            DatasetFileFormatEnum expectedFormat = suppliedRelationshipInputs.getFirst().getFormat();
            boolean sameFormat = suppliedRelationshipInputs
                    .stream()
                    .allMatch(input -> input.getFormat() == expectedFormat);
            if (!sameFormat) {
                throw new ValidationException(
                        ErrorCode.INVALID_INPUTS,
                        "Step " + stepCount + " input relationship SAME_FORMAT requires ports "
                                + String.join(", ", relationship.getPorts()) + " to use the same format"
                );
            }
        }
    }

    private List<ExperimentRunResolvedPlan.Output> resolveOutputs(
            @Nullable List<OutputManifest> outputs,
            @NonNull Map<String, ExperimentRunResolvedPlan.Input> resolvedInputsByPort,
            @NonNull Integer stepCount,
            @NonNull Set<OutputReference> downstreamOutputReferences) {

        if (ExperimentOpsUtils.isEmpty(outputs)) {
            throw new ValidationException(ErrorCode.INVALID_INPUTS, "experimentType.formatMappings.outputs");
        }

        List<ExperimentRunResolvedPlan.Output> resolvedOutputs = new ArrayList<>();
        for (OutputManifest output : outputs) {
            ExperimentRunResolvedPlan.Output resolvedOutput = resolveOutput(
                    output,
                    resolvedInputsByPort,
                    stepCount,
                    downstreamOutputReferences
            );
            if (resolvedOutput != null) {
                resolvedOutputs.add(resolvedOutput);
            }
        }

        if (resolvedOutputs.isEmpty()) {
            throw new ValidationException(ErrorCode.INVALID_INPUTS, "Step " + stepCount + " does not resolve any outputs");
        }
        return resolvedOutputs;
    }

    private ExperimentRunResolvedPlan.Output resolveOutput(
            @Nullable OutputManifest output,
            @NonNull Map<String, ExperimentRunResolvedPlan.Input> resolvedInputsByPort,
            @NonNull Integer stepCount,
            @NonNull Set<OutputReference> downstreamOutputReferences) {

        if (output == null || StringUtils.isBlank(output.getName()) || output.getType() == null || output.getType().getType() == null) {
            throw new ValidationException(ErrorCode.INVALID_INPUTS, "experimentType.formatMappings.outputs");
        }

        FormatStrategy strategy = output.getType();
        boolean requiredForRun = isOutputRequiredForRun(output, stepCount, downstreamOutputReferences);
        DatasetFileFormatEnum outputFormat;
        if (strategy.getType() == FormatStrategyTypeEnum.FIXED) {
            outputFormat = strategy.getFormat();
            if (outputFormat == null) {
                throw new ValidationException(ErrorCode.INVALID_INPUTS, "experimentType.formatMappings.outputs.type.format");
            }
        } else {
            ExperimentRunResolvedPlan.Input sourceInput = resolvedInputsByPort.get(strategy.getSourceInputPort());
            if (sourceInput == null) {
                if (requiredForRun) {
                    throw new ValidationException(
                            ErrorCode.INVALID_INPUTS,
                            "Step " + stepCount + " output " + output.getName()
                                    + " cannot resolve sourceInputPort " + strategy.getSourceInputPort()
                    );
                }
                return null;
            }
            outputFormat = sourceInput.getFormat();
        }

        return ExperimentRunResolvedPlan.Output.builder()
                .name(output.getName())
                .dataKind(output.getDataKind().name())
                .formatStrategy(strategy.getType())
                .format(outputFormat)
                .sourceInputPort(strategy.getSourceInputPort())
                .downStreamPolicy(output.getDownStreamPolicy())
                .requiredForRun(requiredForRun)
                .build();
    }

    private boolean isOutputRequiredForRun(
            @NonNull OutputManifest output,
            @NonNull Integer stepCount,
            @NonNull Set<OutputReference> downstreamOutputReferences) {

        return Boolean.TRUE.equals(output.getRequired())
                || downstreamOutputReferences.contains(new OutputReference(stepCount, output.getName()));
    }

    @NonNull
    private Set<OutputReference> collectDownstreamOutputReferences(@NonNull List<ExperimentRunExecutionModeModel> executionMode) {
        Set<OutputReference> outputReferences = new HashSet<>();
        for (ExperimentRunExecutionModeModel executionModeItem : executionMode) {
            for (ExperimentRunInputModel input : executionModeItem.getInputs()) {
                if (input.getInputType() == ExperimentRunInputModel.InputTypeEnum.ARTIFACT) {
                    outputReferences.add(new OutputReference(getNullableInteger(input.getSourceStepCount()), input.getFile()));
                }
            }
        }
        return outputReferences;
    }

    private void validateExecutionMode(List<ExperimentRunExecutionModeModel> executionMode) {
        if (ExperimentOpsUtils.isEmpty(executionMode)) {
            throw new ValidationException(ErrorCode.REQUIRED_FIELD_MISSING, "executionMode");
        }

        Set<Integer> stepCounts = new HashSet<>();
        for (ExperimentRunExecutionModeModel executionModeItem : executionMode) {
            if (executionModeItem == null) {
                throw new ValidationException(ErrorCode.INVALID_INPUTS, "executionMode");
            }
            if (executionModeItem.getStepCount() == null || executionModeItem.getStepCount() <= 0) {
                throw new ValidationException(ErrorCode.INVALID_INPUTS, "executionMode.stepCount");
            }
            if (!stepCounts.add(executionModeItem.getStepCount())) {
                throw new ValidationException(ErrorCode.INVALID_INPUTS, "executionMode.stepCount should be unique");
            }
            validateInputString("executionMode.experimentConfigUuid", executionModeItem.getExperimentConfigUuid());
            validateExecutionModeInputs(executionModeItem);
        }

        for (int stepCount = 1; stepCount <= executionMode.size(); stepCount++) {
            if (!stepCounts.contains(stepCount)) {
                throw new ValidationException(ErrorCode.INVALID_INPUTS, "executionMode.stepCount should be consecutive starting at 1");
            }
        }
    }

    private void validateExecutionModeInputs(@NonNull ExperimentRunExecutionModeModel executionModeItem) {
        if (ExperimentOpsUtils.isEmpty(executionModeItem.getInputs())) {
            throw new ValidationException(ErrorCode.REQUIRED_FIELD_MISSING, "executionMode.inputs");
        }

        Set<String> portNames = new HashSet<>();
        for (ExperimentRunInputModel input : executionModeItem.getInputs()) {
            if (input == null) {
                throw new ValidationException(ErrorCode.INVALID_INPUTS, "executionMode.inputs");
            }
            validateInputString("executionMode.inputs.portName", input.getPortName());
            if (!portNames.add(input.getPortName())) {
                throw new ValidationException(ErrorCode.INVALID_INPUTS, "executionMode.inputs.portName should be unique per step");
            }
            if (input.getInputType() == null) {
                throw new ValidationException(ErrorCode.REQUIRED_FIELD_MISSING, "executionMode.inputs.inputType");
            }
            validateInputString("executionMode.inputs.file", input.getFile());

            Integer sourceStepCount = getNullableInteger(input.getSourceStepCount());
            if (input.getInputType() == ExperimentRunInputModel.InputTypeEnum.DATASET && sourceStepCount != null) {
                throw new ValidationException(ErrorCode.INVALID_INPUTS, "executionMode.inputs.sourceStepCount is only supported for ARTIFACT inputs");
            }
            if (input.getInputType() == ExperimentRunInputModel.InputTypeEnum.DATASET && executionModeItem.getStepCount() != 1) {
                throw new ValidationException(ErrorCode.INVALID_INPUTS, "executionMode.inputs.DATASET is only supported for stepCount 1");
            }
            if (input.getInputType() == ExperimentRunInputModel.InputTypeEnum.ARTIFACT) {
                if (sourceStepCount == null) {
                    throw new ValidationException(ErrorCode.REQUIRED_FIELD_MISSING, "executionMode.inputs.sourceStepCount");
                }
                if (sourceStepCount <= 0) {
                    throw new ValidationException(ErrorCode.INVALID_INPUTS, "executionMode.inputs.sourceStepCount");
                }
                if (sourceStepCount >= executionModeItem.getStepCount()) {
                    throw new ValidationException(ErrorCode.INVALID_INPUTS, "executionMode.inputs.sourceStepCount should reference an earlier step");
                }
            }
        }
    }

    @NonNull
    private DatasetFileFormatEnum parseFormat(@Nullable String format, @NonNull String field) {
        if (StringUtils.isBlank(format)) {
            throw new ValidationException(ErrorCode.INVALID_INPUTS, field);
        }
        try {
            return DatasetFileFormatEnum.valueOf(format.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException(ErrorCode.INVALID_INPUTS, field);
        }
    }

    @Nullable
    private String normalizeDataKind(@Nullable String dataKind) {
        if (StringUtils.isBlank(dataKind)) {
            return null;
        }
        return dataKind.trim().toUpperCase();
    }

    @Nullable
    private Integer getNullableInteger(@Nullable JsonNullable<Integer> value) {
        if (value == null || !value.isPresent()) {
            return null;
        }
        return value.get();
    }

    private record OutputReference(Integer stepCount, String outputName) {
    }
}
