package com.experimentops.platformapi.interfaces.ctrl;

import com.experimentops.experiment.type.api.v1.ExperimentTypeApi;
import com.experimentops.experiment.type.model.v1.ExperimentTypeListResponseModel;
import com.experimentops.experiment.type.model.v1.ExperimentTypeRequestModel;
import com.experimentops.experiment.type.model.v1.ExperimentTypeResponseModel;
import com.experimentops.experiment.type.model.v1.ExperimentTypeStatusChangeRequestModel;
import com.experimentops.platformapi.service.ExperimentTypeService;
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
public class ExperimentTypeController implements ExperimentTypeApi {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(ExperimentTypeController.class);

    private final HttpServletRequest exchange;
    private final ExperimentTypeService experimentTypeService;

    @PreAuthorize("hasAuthority('" + PermissionConstants.ADD_EXPERIMENT_TYPE + "')")
    @Override
    public ResponseEntity<ExperimentTypeResponseModel> addExperimentType(ExperimentTypeRequestModel experimentTypeRequestModel) {
        ExperimentOpsHeaders headers = HeaderUtil.getHeaders(exchange);
        this.isAdministrator(headers);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(experimentTypeService.publishExperimentTypeCreationEvent(experimentTypeRequestModel, headers));
    }

    @PreAuthorize("hasAuthority('" + PermissionConstants.GET_EXPERIMENT_TYPE + "')")
    @Override
    public ResponseEntity<ExperimentTypeListResponseModel> getExperimentTypeList(String name, Integer page, Integer size) {
        ExperimentOpsHeaders headers = HeaderUtil.getHeaders(exchange);
        this.isAdministrator(headers);
        return ResponseEntity.status(HttpStatus.OK).body(experimentTypeService.getExperimentTypeList(name, page, size, headers));
    }

    @PreAuthorize("hasAuthority('" + PermissionConstants.EDIT_EXPERIMENT_TYPE + "')")
    @Override
    public ResponseEntity<ExperimentTypeResponseModel> updateExperimentType(String uuid, ExperimentTypeRequestModel experimentTypeRequestModel) {
        ExperimentOpsHeaders headers = HeaderUtil.getHeaders(exchange);
        this.isAdministrator(headers);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(experimentTypeService.publishExperimentTypeUpdateEvent(uuid, experimentTypeRequestModel, headers));
    }

    @PreAuthorize("hasAuthority('" + PermissionConstants.EDIT_EXPERIMENT_TYPE + "')")
    @Override
    public ResponseEntity<ExperimentTypeResponseModel> updateStatusExperimentType(String uuid, ExperimentTypeStatusChangeRequestModel experimentTypeStatusChangeRequestModel) {
        ExperimentOpsHeaders headers = HeaderUtil.getHeaders(exchange);
        this.isAdministrator(headers);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(experimentTypeService.publishExperimentTypeStatusChangeEvent(uuid, experimentTypeStatusChangeRequestModel, headers));
    }

    private void isAdministrator(ExperimentOpsHeaders headers){
        if (HeaderUtil.isAdministrator(headers)){
            headers.setWorkspaceUuid(null);
        }
    }

}
