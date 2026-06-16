package com.experimentops.platformapi.validator;

import com.experimentops.dataset.model.v1.DatasetRequestModel;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

@Component
public class DatasetValidator extends GenericValidator {

    public void validateDatasetRequestModel(@NonNull DatasetRequestModel requestModel) {
        validateInputString("name", requestModel.getName());
    }

}
