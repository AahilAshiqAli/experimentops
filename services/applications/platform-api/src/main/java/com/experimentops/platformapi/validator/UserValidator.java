package com.experimentops.platformapi.validator;

import com.experimentops.common.exceptions.constant.ErrorCode;
import com.experimentops.common.exceptions.runtime.ValidationException;
import com.experimentops.user.model.v1.UserRequestModel;
import com.experimentops.utils.constant.RoleType;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

@Component
public class UserValidator extends GenericValidator {

    public void validateUserRequestModel(@NonNull UserRequestModel requestModel) {
        validateInputString("email", requestModel.getEmail());
        validateInputString("password", requestModel.getPassword());
        validateInputString("firstName", requestModel.getFirstName());
        validateInputString("lastName", requestModel.getLastName());
        validateInputString("userRole", requestModel.getUserRole());

        if (!RoleType.isValid(requestModel.getUserRole())){
            throw new ValidationException(ErrorCode.INVALID_INPUTS, "user role should be one of WORKSPACE_ADMIN or RESEARCHER");
        }
    }
}
