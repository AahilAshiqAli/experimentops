package com.experimentops.common.exceptions.runtime;

import com.experimentops.common.exceptions.ExperimentOpsException;
import com.experimentops.common.exceptions.constant.ErrorCode;

public class EntityNotFoundException extends ExperimentOpsException {
    public EntityNotFoundException(String entityName, String id) {
        super(
                ErrorCode.ENTITY_NOT_FOUND,
                entityName + " not found for id: " + id
        );
    }
    public EntityNotFoundException(String message){
        super(
               ErrorCode.ENTITY_NOT_FOUND,
               message
        );
    }
}