package com.experimentops.platformapi.validator;

import com.experimentops.common.exceptions.constant.ErrorCode;
import com.experimentops.common.exceptions.runtime.ValidationException;
import com.experimentops.dataset.model.v1.DatasetRequestModel;
import com.experimentops.dataset.model.v1.DatasetVersionStatusChangeRequestModel;
import com.experimentops.dataset.model.v1.DatasetVersionUploadRequestModel;
import com.experimentops.platformapi.model.type.StatusEnum;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

@Component
public class DatasetValidator extends GenericValidator {

    public void validateDatasetRequestModel(@NonNull DatasetRequestModel requestModel) {
        validateInputString("name", requestModel.getName());
    }

    public void validateDatasetVersionUploadRequest(@NonNull DatasetVersionUploadRequestModel requestModel) {
        if (requestModel.getFileName() == null || requestModel.getFileName().isBlank()) {
            throw new ValidationException(ErrorCode.INVALID_INPUTS, "Dataset file name is required");
        }
    }

    public StatusEnum validateDatasetVersionStatusChangeRequestModel(@NonNull DatasetVersionStatusChangeRequestModel requestModel) {
        StatusEnum status = StatusEnum.of(requestModel.getStatus().getValue());
        if (status == null) {
            throw new ValidationException(ErrorCode.INVALID_INPUTS, "status");
        }
        if (StatusEnum.ACTIVE != status && StatusEnum.FAILED != status) {
            throw new ValidationException(ErrorCode.INVALID_INPUTS, "Dataset version status must be ACTIVE or FAILED");
        }
        if (StatusEnum.FAILED == status &&
                (requestModel.getFailureMessage() == null || requestModel.getFailureMessage().isBlank())) {
            throw new ValidationException(ErrorCode.INVALID_INPUTS, "failureMessage is required when status is FAILED");
        }
        return status;
    }
}
