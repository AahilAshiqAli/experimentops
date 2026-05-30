package com.experimentops.platformapi.validator;

import com.experimentops.common.exceptions.constant.ErrorCode;
import com.experimentops.common.exceptions.runtime.ValidationException;
import com.experimentops.user.model.v1.AuthLoginRequest;
import org.springframework.stereotype.Component;

import static com.experimentops.utils.ExperimentOpsUtils.validateInputString;

@Component
public class AuthValidator {

    public void validateAuthLoginRequestModel(AuthLoginRequest authLoginRequest) {
        if (authLoginRequest == null) {
            throw new ValidationException(ErrorCode.REQUIRED_FIELD_MISSING, "authLoginRequest");
        }

        validateInputString("username", authLoginRequest.getUsername());
        validateInputString("password", authLoginRequest.getPassword());
        validateInputString("client_id", authLoginRequest.getClientId());
    }

}
