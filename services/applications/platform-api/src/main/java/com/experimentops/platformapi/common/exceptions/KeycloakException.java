package com.experimentops.platformapi.common.exceptions;

import com.experimentops.common.exceptions.ExperimentOpsException;
import com.experimentops.common.exceptions.constant.ErrorCode;
import lombok.ToString;

@ToString
public class KeycloakException extends ExperimentOpsException {

    public KeycloakException(ErrorCode errorCode, Exception exception) {
        super(errorCode, errorCode.getMessage()+String.format(" Cause: %s",exception));
    }

}
