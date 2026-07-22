package com.experimentops.platformapi.interfaces.ctrl;

import com.experimentops.platformapi.service.RunArtifactService;
import com.experimentops.run.artifact.api.v1.RunArtifactApi;
import com.experimentops.run.artifact.model.v1.RunArtifactDownloadUrlResponseModel;
import com.experimentops.run.artifact.model.v1.RunArtifactListResponseModel;
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
public class RunArtifactController implements RunArtifactApi {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(RunArtifactController.class);

    private final HttpServletRequest exchange;
    private final RunArtifactService runArtifactService;

    @PreAuthorize("hasAuthority('" + PermissionConstants.GET_EXPERIMENT_RUNS + "')")
    @Override
    public ResponseEntity<RunArtifactListResponseModel> getExperimentArtifacts(String experimentRunUuid) {
        ExperimentOpsHeaders headers = HeaderUtil.getHeaders(exchange);
        return ResponseEntity.status(HttpStatus.OK).body(runArtifactService.getExperimentArtifacts(experimentRunUuid, headers));
    }

    @PreAuthorize("hasAuthority('" + PermissionConstants.GET_EXPERIMENT_RUNS + "')")
    @Override
    public ResponseEntity<RunArtifactDownloadUrlResponseModel> getRunArtifactDownloadUrl(String artifactUuid) {
        ExperimentOpsHeaders headers = HeaderUtil.getHeaders(exchange);
        return ResponseEntity.status(HttpStatus.OK).body(runArtifactService.getRunArtifactDownloadUrl(artifactUuid, headers));
    }
}
