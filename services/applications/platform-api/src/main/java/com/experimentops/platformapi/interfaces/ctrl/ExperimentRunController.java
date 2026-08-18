package com.experimentops.platformapi.interfaces.ctrl;

import com.experimentops.experiment.run.api.v1.ExperimentRunApi;
import com.experimentops.experiment.run.model.v1.*;
import com.experimentops.platformapi.service.ExperimentRunService;
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

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("v1/")
public class ExperimentRunController implements ExperimentRunApi {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(ExperimentRunController.class);

    private final HttpServletRequest exchange;
    private final ExperimentRunService experimentRunService;

    @PreAuthorize("hasAuthority('" + PermissionConstants.RUN_EXPERIMENT + "')")
    @Override
    public ResponseEntity<ExperimentRunResponseModel> addExperimentRun(String experimentUuid, ExperimentRunRequestModel experimentRunRequestModel) {
        ExperimentOpsHeaders experimentOpsHeaders = HeaderUtil.getHeaders(exchange);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(experimentRunService.publishExperimentRunRequest(experimentUuid, experimentRunRequestModel, experimentOpsHeaders));
    }

    @PreAuthorize("hasAuthority('" + PermissionConstants.COMPARE_EXPERIMENT_RUNS + "')")
    @Override
    public ResponseEntity<ExperimentRunCompareResponseModel> compareExperimentRun(ExperimentRunCompareRequestModel experimentRunCompareRequestModel) {
        ExperimentOpsHeaders experimentOpsHeaders = HeaderUtil.getHeaders(exchange);
        return ResponseEntity.status(HttpStatus.OK).body(experimentRunService.compareExperiment(experimentRunCompareRequestModel, experimentOpsHeaders));
    }

    @PreAuthorize("hasAuthority('" + PermissionConstants.RUN_EXPERIMENT + "')")
    @Override
    public ResponseEntity<Void> validateExperimentRun(String experimentUuid, ExperimentRunRequestModel experimentRunRequestModel) {
        ExperimentOpsHeaders experimentOpsHeaders = HeaderUtil.getHeaders(exchange);
        experimentRunService.validateExperimentRunRequest(experimentUuid, experimentRunRequestModel, experimentOpsHeaders);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PreAuthorize("hasAuthority('" + PermissionConstants.GET_EXPERIMENT_RUNS + "')")
    @Override
    public ResponseEntity<ExperimentRunDetailResponseModel> getExperimentRun(String uuid) {
        ExperimentOpsHeaders experimentOpsHeaders = HeaderUtil.getHeaders(exchange);
        return ResponseEntity.status(HttpStatus.OK).body(experimentRunService.getExperimentRun(uuid, experimentOpsHeaders));
    }

    @PreAuthorize("hasAuthority('" + PermissionConstants.GET_EXPERIMENT_RUNS + "')")
    @Override
    public ResponseEntity<ExperimentRunListResponseModel> getExperimentRunList(String experimentUuid, String name, String status, Integer page, Integer size) {
        ExperimentOpsHeaders experimentOpsHeaders = HeaderUtil.getHeaders(exchange);
        return ResponseEntity.status(HttpStatus.OK).body(experimentRunService.getExperimentRunList(experimentUuid, name, status, page, size, experimentOpsHeaders));
    }

    @PreAuthorize("hasAuthority('" + PermissionConstants.GET_EXPERIMENT_RUNS + "')")
    @Override
    public ResponseEntity<List<ExperimentRunStatusResponseModel>> getExperimentRunStatus(ExperimentRunStatusRequestModel experimentRunStatusRequestModel) {
        ExperimentOpsHeaders experimentOpsHeaders = HeaderUtil.getHeaders(exchange);
        return ResponseEntity.status(HttpStatus.OK).body(experimentRunService.getExperimentRunStatus(experimentRunStatusRequestModel, experimentOpsHeaders));
    }
}
