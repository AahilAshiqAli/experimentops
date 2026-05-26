package com.experimentops.common.exceptions;

import com.experimentops.common.exceptions.constant.ErrorCode;

public abstract class ExperimentOpsException extends RuntimeException {
    private final ErrorCode errorCode;

    protected ExperimentOpsException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    protected ExperimentOpsException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected ExperimentOpsException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
