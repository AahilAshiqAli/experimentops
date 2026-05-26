package com.experimentops.common.exceptions.runtime;

import com.experimentops.common.exceptions.ExperimentOpsException;
import com.experimentops.common.exceptions.constant.ErrorCode;

public class ExperimentOpsGatewayException extends ExperimentOpsException {
    public ExperimentOpsGatewayException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public ExperimentOpsGatewayException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
