package com.experimentops.platformapi.interfaces.ctrl;

import com.experimentops.experiment.run.api.v1.ExperimentRunLogApi;
import com.experimentops.experiment.run.model.v1.ExperimentRunLogDownloadUrlResponseModel;
import com.experimentops.experiment.run.model.v1.ExperimentRunLogListResponseModel;
import com.experimentops.platformapi.service.ExperimentRunLogService;
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
public class ExperimentRunLogController implements ExperimentRunLogApi {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(ExperimentRunLogController.class);

    private final HttpServletRequest exchange;
    private final ExperimentRunLogService experimentRunLogService;

    @PreAuthorize("hasAuthority('" + PermissionConstants.GET_EXPERIMENT_RUNS + "')")
    @Override
    public ResponseEntity<ExperimentRunLogListResponseModel> getExperimentRunLogs(String uuid, Integer page, Integer size) {
        ExperimentOpsHeaders headers = HeaderUtil.getHeaders(exchange);
        return ResponseEntity.status(HttpStatus.OK).body(experimentRunLogService.getExperimentRunLogs(uuid, page, size, headers));
    }

    @PreAuthorize("hasAuthority('" + PermissionConstants.GET_EXPERIMENT_RUNS + "')")
    @Override
    public ResponseEntity<ExperimentRunLogDownloadUrlResponseModel> getExperimentRunLogDownloadUrl(String uuid) {
        ExperimentOpsHeaders headers = HeaderUtil.getHeaders(exchange);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(experimentRunLogService.getExperimentRunLogDownloadUrl(uuid, headers));
    }
}
