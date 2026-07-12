package com.experimentops.platformapi.validator;

import com.experimentops.common.exceptions.constant.ErrorCode;
import com.experimentops.common.exceptions.runtime.ValidationException;
import com.experimentops.experiment.run.model.v1.ExperimentRunExecutionModeModel;
import com.experimentops.experiment.run.model.v1.ExperimentRunRequestModel;
import com.experimentops.experiment.run.model.v1.ExperimentRunStatusRequestModel;
import com.experimentops.platformapi.model.ExperimentRunConfigContext;
import com.experimentops.platformapi.model.entity.ExperimentTypeFormatMapping;
import com.experimentops.platformapi.model.type.ExperimentStatusEnum;
import com.experimentops.utils.ExperimentOpsUtils;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ExperimentRunValidator extends GenericValidator {

    public void validateExperimentRunRequestModel(@NonNull ExperimentRunRequestModel requestModel) {
        validateInputString("datasetVersionUuid", requestModel.getDatasetVersionUuid());
        validateExecutionMode(requestModel.getExecutionMode());
    }

    public void validateExperimentRunRequestModel(@NonNull String experimentUuid, @NonNull ExperimentRunRequestModel requestModel) {
        validateInputString("experimentUuid", experimentUuid);
        validateExperimentRunRequestModel(requestModel);
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
    public String validateInputOutputForExecutionMode(
            @NonNull List<ExperimentRunExecutionModeModel> executionMode,
            String datasetFormat,
            @NonNull Map<String, ExperimentRunConfigContext> experimentConfigsByUuid) {

        if (StringUtils.isBlank(datasetFormat)) {
            throw new ValidationException(ErrorCode.INVALID_INPUTS, "datasetVersion.format");
        }

        String currentInputFormat = datasetFormat.trim().toUpperCase();
        List<ExperimentRunExecutionModeModel> sortedExecutionMode = executionMode
                .stream()
                .sorted(Comparator.comparing(ExperimentRunExecutionModeModel::getStepCount))
                .toList();

        for (ExperimentRunExecutionModeModel executionModeItem : sortedExecutionMode) {
            ExperimentRunConfigContext experimentConfig = experimentConfigsByUuid.get(executionModeItem.getExperimentConfigUuid());
            if (experimentConfig == null) {
                throw new ValidationException(ErrorCode.INVALID_INPUTS, "executionMode.experimentConfigUuid");
            }

            ExperimentTypeFormatMapping mapping = findFormatMapping(
                    experimentConfig,
                    currentInputFormat,
                    executionModeItem.getStepCount()
            );
            currentInputFormat = mapping.getOutputFormat().name();
        }

        return currentInputFormat;
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
        }

        for (int stepCount = 1; stepCount <= executionMode.size(); stepCount++) {
            if (!stepCounts.contains(stepCount)) {
                throw new ValidationException(ErrorCode.INVALID_INPUTS, "executionMode.stepCount should be consecutive starting at 1");
            }
        }
    }

    @NonNull
    private ExperimentTypeFormatMapping findFormatMapping(
            @NonNull ExperimentRunConfigContext experimentConfig,
            @NonNull String inputFormat,
            Integer stepCount) {

        List<ExperimentTypeFormatMapping> formatMappings = experimentConfig.getFormatMappings();
        if (ExperimentOpsUtils.isEmpty(formatMappings)) {
            throw new ValidationException(
                    ErrorCode.INVALID_INPUTS,
                    "experimentType.formatMappings"
            );
        }

        return formatMappings
                .stream()
                .filter(formatMapping -> formatMapping != null
                        && formatMapping.getInputFormat() != null
                        && formatMapping.getOutputFormat() != null
                        && inputFormat.equals(formatMapping.getInputFormat().name()))
                .findFirst()
                .orElseThrow(() -> new ValidationException(
                        ErrorCode.INVALID_INPUTS,
                        "Step " + stepCount + " experiment type " + experimentConfig.getExperimentType()
                                + " does not accept input format " + inputFormat
                ));
    }
}
