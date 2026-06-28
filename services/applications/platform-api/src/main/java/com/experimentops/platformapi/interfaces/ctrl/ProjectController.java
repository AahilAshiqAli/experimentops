package com.experimentops.platformapi.interfaces.ctrl;

import com.experimentops.platformapi.service.ProjectService;
import com.experimentops.project.api.v1.ProjectApi;
import com.experimentops.project.model.v1.ProjectListResponseModel;
import com.experimentops.project.model.v1.ProjectRequestModel;
import com.experimentops.project.model.v1.ProjectResponseModel;
import com.experimentops.project.model.v1.ProjectSummaryResponseModel;
import com.experimentops.project.model.v1.ProjectStatusChangeRequestModel;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.HeaderUtil;
import com.experimentops.utils.constant.PermissionConstants;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
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
public class ProjectController implements ProjectApi {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(ProjectController.class);

    private final HttpServletRequest exchange;
    private final ProjectService projectService;

    @PreAuthorize("hasAuthority('" + PermissionConstants.ADD_PROJECT + "')")
    @Override
    public ResponseEntity<ProjectResponseModel> addProject(ProjectRequestModel projectRequestModel) {
        ExperimentOpsHeaders experimentOpsHeaders = HeaderUtil.getHeaders(exchange);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(projectService.publishProjectCreationEvent(projectRequestModel, experimentOpsHeaders));
    }

    @PreAuthorize("hasAuthority('" + PermissionConstants.GET_PROJECT + "')")
    @Override
    public ResponseEntity<ProjectListResponseModel> getProjectList(Integer page, Integer size) {
        ExperimentOpsHeaders experimentOpsHeaders = HeaderUtil.getHeaders(exchange);
        return ResponseEntity.status(HttpStatus.OK).body(projectService.getProjectList(page, size, experimentOpsHeaders));
    }

    @PreAuthorize("hasAuthority('" + PermissionConstants.GET_PROJECT + "')")
    @Override
    public ResponseEntity<ProjectSummaryResponseModel> getProject(String uuid) {
        ExperimentOpsHeaders experimentOpsHeaders = HeaderUtil.getHeaders(exchange);
        return ResponseEntity.status(HttpStatus.OK).body(projectService.getProject(uuid, experimentOpsHeaders));
    }

    @PreAuthorize("hasAuthority('" + PermissionConstants.EDIT_PROJECT + "')")
    @Override
    public ResponseEntity<ProjectResponseModel> updateProject(String uuid, ProjectRequestModel projectRequestModel) {
        ExperimentOpsHeaders experimentOpsHeaders = HeaderUtil.getHeaders(exchange);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(projectService.publishProjectUpdateEvent(uuid, projectRequestModel, experimentOpsHeaders));
    }

    @PreAuthorize("hasAuthority('" + PermissionConstants.EDIT_PROJECT + "')")
    @Override
    public ResponseEntity<ProjectResponseModel> updateStatusProject(String uuid, ProjectStatusChangeRequestModel projectStatusChangeRequestModel) {
        ExperimentOpsHeaders experimentOpsHeaders = HeaderUtil.getHeaders(exchange);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(projectService.publishProjectStatusChangeEvent(uuid, projectStatusChangeRequestModel, experimentOpsHeaders));
    }

}
