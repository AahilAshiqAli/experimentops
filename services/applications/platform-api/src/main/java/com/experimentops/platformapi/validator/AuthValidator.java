package com.experimentops.platformapi.validator;

import com.experimentops.common.exceptions.constant.ErrorCode;
import com.experimentops.common.exceptions.runtime.ValidationException;
import com.experimentops.user.model.v1.AuthForgotPasswordRequest;
import com.experimentops.user.model.v1.AuthLoginRequest;
import com.experimentops.user.model.v1.AuthResetPasswordRequest;
import org.springframework.stereotype.Component;

@Component
public class AuthValidator extends GenericValidator {

    public void validateAuthLoginRequestModel(AuthLoginRequest authLoginRequest) {
        if (authLoginRequest == null) {
            throw new ValidationException(ErrorCode.REQUIRED_FIELD_MISSING, "authLoginRequest");
        }

        validateInputString("username", authLoginRequest.getUsername());
        validateInputString("password", authLoginRequest.getPassword());
    }

    public void validateAuthForgotPasswordRequestModel(AuthForgotPasswordRequest request) {
        if (request == null) {
            throw new ValidationException(ErrorCode.REQUIRED_FIELD_MISSING, "authForgotPasswordRequest");
        }

        validateInputString("workspaceName", request.getWorkspaceName());
        validateInputString("email", request.getEmail());
    }

    public void validateAuthResetPasswordRequestModel(AuthResetPasswordRequest request) {
        if (request == null) {
            throw new ValidationException(ErrorCode.REQUIRED_FIELD_MISSING, "authResetPasswordRequest");
        }

        validateInputString("token", request.getToken());
        validateInputString("newPassword", request.getNewPassword());
    }
}
