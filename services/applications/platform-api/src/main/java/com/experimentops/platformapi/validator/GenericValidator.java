package com.experimentops.platformapi.validator;

import com.experimentops.common.exceptions.constant.ErrorCode;
import com.experimentops.common.exceptions.runtime.ValidationException;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;

public class GenericValidator {

    public void validateInputString(@NonNull String key, String value) {
        if (StringUtils.isBlank(value)) {
            throw new ValidationException(ErrorCode.REQUIRED_FIELD_MISSING, key);
        }
    }
}
