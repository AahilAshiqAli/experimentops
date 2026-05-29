package com.experimentops.platformapi.interfaces.ctrl;

import com.experimentops.platformapi.service.WorkspaceService;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.HeaderUtil;
import com.experimentops.utils.constant.PermissionConstants;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import com.experimentops.workspace.api.v1.WorkspacesApi;
import com.experimentops.workspace.model.v1.WorkspaceRequestModel;
import com.experimentops.workspace.model.v1.WorkspaceResponseModel;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("v1/")
public class WorkspaceController implements WorkspacesApi {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(WorkspaceController.class);

    private final HttpServletRequest exchange;
    private final WorkspaceService workspaceService;

    @PreAuthorize("hasAuthority('" + PermissionConstants.ADD_WORKSPACE + "')")
    @Override
    public ResponseEntity<WorkspaceResponseModel> createWorkspace(WorkspaceRequestModel workspaceRequestModel) {
        ExperimentOpsHeaders experimentOpsHeaders = HeaderUtil.getHeaders(exchange);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(workspaceService.publishWorkspaceCreationEvent(workspaceRequestModel, experimentOpsHeaders));
    }


}
