package com.experimentops.platformapi.validator;

import com.experimentops.experiment.run.model.v1.ExperimentRunRequestModel;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

@Component
public class ExperimentRunValidator extends GenericValidator {

    public void validateExperimentRunRequestModel(@NonNull ExperimentRunRequestModel requestModel) {
        validateInputString("datasetVersionUuid", requestModel.getDatasetVersionUuid());
    }

    public void validateExperimentRunRequestModel(@NonNull String experimentUuid, @NonNull ExperimentRunRequestModel requestModel) {
        validateInputString("experimentUuid", experimentUuid);
        validateExperimentRunRequestModel(requestModel);
    }
}
