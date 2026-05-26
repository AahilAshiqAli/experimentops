package com.experimentops.common.exceptions.runtime;

import com.experimentops.common.exceptions.ExperimentOpsException;
import com.experimentops.common.exceptions.constant.ErrorCode;

public class EntityAlreadyExistsException extends ExperimentOpsException {
    public EntityAlreadyExistsException(String entityName, String value) {
        super(
                ErrorCode.ENTITY_ALREADY_EXISTS,
                entityName + " already exists for value: " + value
        );
    }
}
