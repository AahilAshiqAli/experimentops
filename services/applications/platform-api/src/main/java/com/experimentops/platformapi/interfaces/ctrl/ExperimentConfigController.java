package com.experimentops.platformapi.interfaces.ctrl;

import com.experimentops.experiment.api.v1.ExperimentConfigApi;
import com.experimentops.experiment.model.v1.ExperimentConfigListResponseModel;
import com.experimentops.experiment.model.v1.ExperimentConfigRequestModel;
import com.experimentops.experiment.model.v1.ExperimentConfigResponseModel;
import com.experimentops.experiment.model.v1.ExperimentConfigStatusChangeRequestModel;
import com.experimentops.platformapi.service.ExperimentConfigService;
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
public class ExperimentConfigController implements ExperimentConfigApi {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(ExperimentConfigController.class);

    private final HttpServletRequest exchange;
    private final ExperimentConfigService experimentConfigService;

    @PreAuthorize("hasAuthority('" + PermissionConstants.ADD_EXPERIMENT_CONFIG + "')")
    @Override
    public ResponseEntity<ExperimentConfigResponseModel> addExperimentConfig(String experimentUuid, ExperimentConfigRequestModel experimentConfigRequestModel) {
        ExperimentOpsHeaders headers = HeaderUtil.getHeaders(exchange);
        return ResponseEntity.status(HttpStatus.CREATED).body(experimentConfigService.createExperimentConfig(experimentUuid, experimentConfigRequestModel, headers));
    }

    @PreAuthorize("hasAuthority('" + PermissionConstants.GET_EXPERIMENT_CONFIG + "')")
    @Override
    public ResponseEntity<ExperimentConfigResponseModel> getExperimentConfig(String experimentUuid, String uuid) {
        ExperimentOpsHeaders headers = HeaderUtil.getHeaders(exchange);
        return ResponseEntity.status(HttpStatus.OK).body(experimentConfigService.getExperimentConfig(experimentUuid, uuid, headers));
    }

    @PreAuthorize("hasAuthority('" + PermissionConstants.GET_EXPERIMENT_CONFIG + "')")
    @Override
    public ResponseEntity<ExperimentConfigListResponseModel> getExperimentConfigList(String experimentUuid, Integer page, Integer size, String experimentType) {
        ExperimentOpsHeaders headers = HeaderUtil.getHeaders(exchange);
        return ResponseEntity.status(HttpStatus.OK).body(experimentConfigService.getExperimentConfigList(experimentUuid, page, size, experimentType, headers));
    }

    @PreAuthorize("hasAuthority('" + PermissionConstants.EDIT_EXPERIMENT_CONFIG + "')")
    @Override
    public ResponseEntity<ExperimentConfigResponseModel> updateExperimentConfig(String experimentUuid, String uuid, ExperimentConfigRequestModel experimentConfigRequestModel) {
        ExperimentOpsHeaders headers = HeaderUtil.getHeaders(exchange);
        return ResponseEntity.status(HttpStatus.OK).body(experimentConfigService.updateExperimentConfig(experimentUuid, uuid, experimentConfigRequestModel, headers));
    }

    @PreAuthorize("hasAuthority('" + PermissionConstants.EDIT_EXPERIMENT_CONFIG + "')")
    @Override
    public ResponseEntity<ExperimentConfigResponseModel> updateStatusExperimentConfig(String experimentUuid, String uuid, ExperimentConfigStatusChangeRequestModel experimentConfigStatusChangeRequestModel) {
        ExperimentOpsHeaders headers = HeaderUtil.getHeaders(exchange);
        return ResponseEntity.status(HttpStatus.OK).body(experimentConfigService.updateStatusExperimentConfig(experimentUuid, uuid, experimentConfigStatusChangeRequestModel, headers));
    }
}
