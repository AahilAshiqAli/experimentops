package com.experimentops.platformapi.validator;

import com.experimentops.common.exceptions.constant.ErrorCode;
import com.experimentops.common.exceptions.runtime.ValidationException;
import com.experimentops.experiment.run.model.v1.ExperimentRunExecutionModeModel;
import com.experimentops.experiment.run.model.v1.ExperimentRunRequestModel;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
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

    private void validateExecutionMode(List<ExperimentRunExecutionModeModel> executionMode) {
        if (executionMode == null || executionMode.isEmpty()) {
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
}
