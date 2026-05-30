package com.experimentops.platformapi.validator;

import com.experimentops.common.exceptions.constant.ErrorCode;
import com.experimentops.common.exceptions.runtime.ValidationException;
import com.experimentops.workspace.model.v1.WorkspaceAdminUserRequestModel;
import com.experimentops.workspace.model.v1.WorkspaceRequestModel;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import static com.experimentops.utils.ExperimentOpsUtils.validateInputString;

@Component
public class WorkspaceValidator {

    public void validateWorkspaceRequestModel(@NonNull WorkspaceRequestModel requestModel){
        validateInputString("workspaceName", requestModel.getWorkspaceName());
        validateInputString("workspaceEmail", requestModel.getWorkspaceEmail());
        WorkspaceAdminUserRequestModel adminUserRequestModel = requestModel.getAdminUser();
        if (adminUserRequestModel == null) {
            throw new ValidationException(ErrorCode.REQUIRED_FIELD_MISSING, "adminUser");
        }
        else {
            validateInputString("firstName", adminUserRequestModel.getFirstName());
            validateInputString("lastName", adminUserRequestModel.getLastName());
            validateInputString("adminEmail", adminUserRequestModel.getEmail());
        }
    }

}
