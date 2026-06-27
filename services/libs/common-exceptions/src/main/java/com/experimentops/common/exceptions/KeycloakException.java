package com.experimentops.common.exceptions;

import com.experimentops.common.exceptions.constant.ErrorCode;
import lombok.Getter;
import lombok.ToString;

@ToString
public class KeycloakException extends ExperimentOpsException {

    @Getter
    private final String errorToken;

    public KeycloakException(ErrorCode errorCode){
        super(errorCode);
        this.errorToken = null;
    }

    public KeycloakException(ErrorCode errorCode, String username, Exception exception) {
        super(errorCode, errorCode.getMessage()+String.format(" for: %s",username));
        this.errorToken = null;
    }

    public KeycloakException(ErrorCode errorCode, Exception exception) {
        super(errorCode, errorCode.getMessage()+String.format(" for: %s",exception));
        this.errorToken = null;
    }

    public KeycloakException(ErrorCode errorCode, String errorToken){
        super(errorCode);
        this.errorToken = errorToken;
    }

}
