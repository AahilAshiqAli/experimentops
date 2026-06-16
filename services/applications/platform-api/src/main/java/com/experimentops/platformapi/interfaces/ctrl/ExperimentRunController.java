package com.experimentops.platformapi.interfaces.ctrl;

import com.experimentops.experiment.api.v1.ExperimentApi;
import com.experimentops.experiment.run.api.v1.ExperimentRunApi;
import com.experimentops.experiment.run.model.v1.ExperimentRunRequestModel;
import com.experimentops.experiment.run.model.v1.ExperimentRunResponseModel;
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
}
