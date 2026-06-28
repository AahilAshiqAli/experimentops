package com.experimentops.platformapi.interfaces.ctrl;

import com.experimentops.experiment.api.v1.ExperimentApi;
import com.experimentops.experiment.model.v1.ExperimentListResponseModel;
import com.experimentops.experiment.model.v1.ExperimentListItemModel;
import com.experimentops.experiment.model.v1.ExperimentRequestModel;
import com.experimentops.experiment.model.v1.ExperimentResponseModel;
import com.experimentops.experiment.model.v1.ExperimentStatusChangeRequestModel;
import com.experimentops.platformapi.service.ExperimentService;
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
public class ExperimentController implements ExperimentApi {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(ExperimentController.class);

    private final HttpServletRequest exchange;
    private final ExperimentService experimentService;

    @PreAuthorize("hasAuthority('" + PermissionConstants.ADD_EXPERIMENT + "')")
    @Override
    public ResponseEntity<ExperimentResponseModel> addExperiment(String projectUuid, ExperimentRequestModel experimentRequestModel) {
        ExperimentOpsHeaders experimentOpsHeaders = HeaderUtil.getHeaders(exchange);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(experimentService.publishExperimentCreationEvent(projectUuid, experimentRequestModel, experimentOpsHeaders));
    }

    @PreAuthorize("hasAuthority('" + PermissionConstants.GET_EXPERIMENT + "')")
    @Override
    public ResponseEntity<ExperimentResponseModel> getExperiment(String projectUuid, String uuid) {
        ExperimentOpsHeaders experimentOpsHeaders = HeaderUtil.getHeaders(exchange);
        return ResponseEntity.status(HttpStatus.OK).body(experimentService.getExperiment(uuid, projectUuid, experimentOpsHeaders));
    }

    @PreAuthorize("hasAuthority('" + PermissionConstants.GET_EXPERIMENT + "')")
    @Override
    public ResponseEntity<ExperimentListResponseModel> getExperimentList(String projectUuid, Integer page, Integer size) {
        ExperimentOpsHeaders experimentOpsHeaders = HeaderUtil.getHeaders(exchange);
        return ResponseEntity.status(HttpStatus.OK).body(experimentService.getExperimentList(projectUuid, page, size, experimentOpsHeaders));
    }

    @PreAuthorize("hasAuthority('" + PermissionConstants.EDIT_EXPERIMENT + "')")
    @Override
    public ResponseEntity<ExperimentResponseModel> updateExperiment(String projectUuid, String uuid, ExperimentRequestModel experimentRequestModel) {
        ExperimentOpsHeaders experimentOpsHeaders = HeaderUtil.getHeaders(exchange);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(experimentService.publishExperimentUpdateEvent(uuid, projectUuid, experimentRequestModel, experimentOpsHeaders));
    }

    @PreAuthorize("hasAuthority('" + PermissionConstants.EDIT_EXPERIMENT + "')")
    @Override
    public ResponseEntity<ExperimentResponseModel> updateStatusExperiment(String projectUuid, String uuid, ExperimentStatusChangeRequestModel experimentStatusChangeRequestModel) {
        ExperimentOpsHeaders experimentOpsHeaders = HeaderUtil.getHeaders(exchange);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(experimentService.publishExperimentStatusChangeEvent(uuid, projectUuid, experimentStatusChangeRequestModel, experimentOpsHeaders));
    }

}
