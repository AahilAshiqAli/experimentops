package com.experimentops.platformapi.validator;

import com.experimentops.project.model.v1.ProjectRequestModel;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

@Component
public class ProjectValidator extends GenericValidator {

    public void validateProjectRequestModel(@NonNull ProjectRequestModel requestModel) {
        validateInputString("name", requestModel.getName());
        validateInputString("description", requestModel.getDescription());
    }

}
