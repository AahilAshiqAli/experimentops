package com.experimentops.platformapi.validator;

import com.experimentops.experiment.model.v1.ExperimentRequestModel;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

@Component
public class ExperimentValidator extends GenericValidator {

    public void validateExperimentRequestModel(@NonNull ExperimentRequestModel requestModel) {
        validateInputString("name", requestModel.getName());
    }

}
