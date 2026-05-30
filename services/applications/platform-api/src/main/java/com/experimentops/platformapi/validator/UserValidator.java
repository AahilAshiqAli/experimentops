package com.experimentops.platformapi.validator;

import com.experimentops.user.model.v1.UserRequestModel;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import static com.experimentops.utils.ExperimentOpsUtils.validateInputString;

@Component
public class UserValidator {

    public void validateUserRequestModel(@NonNull UserRequestModel requestModel) {
        validateInputString("email", requestModel.getEmail());
        validateInputString("password", requestModel.getPassword());
        validateInputString("firstName", requestModel.getFirstName());
        validateInputString("lastName", requestModel.getLastName());
        validateInputString("userRole", requestModel.getUserRole());
    }

}
