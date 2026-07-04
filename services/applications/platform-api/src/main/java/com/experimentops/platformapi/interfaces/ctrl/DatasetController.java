package com.experimentops.platformapi.interfaces.ctrl;

import com.experimentops.dataset.api.v1.DatasetApi;
import com.experimentops.dataset.model.v1.DatasetDetailResponseModel;
import com.experimentops.dataset.model.v1.DatasetListResponseModel;
import com.experimentops.dataset.model.v1.DatasetRequestModel;
import com.experimentops.dataset.model.v1.DatasetResponseModel;
import com.experimentops.dataset.model.v1.DatasetStatusChangeRequestModel;
import com.experimentops.dataset.model.v1.DatasetVersionItemModel;
import com.experimentops.dataset.model.v1.DatasetVersionResponseModel;
import com.experimentops.dataset.model.v1.DatasetVersionStatusChangeRequestModel;
import com.experimentops.dataset.model.v1.DatasetVersionUploadRequestModel;
import com.experimentops.platformapi.service.DatasetService;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.HeaderUtil;
import com.experimentops.utils.constant.PermissionConstants;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("v1/")
public class DatasetController implements DatasetApi {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(DatasetController.class);

    private final HttpServletRequest exchange;
    private final DatasetService datasetService;

    @PreAuthorize("hasAuthority('" + PermissionConstants.ADD_DATASET + "')")
    @Override
    public ResponseEntity<DatasetResponseModel> addDataset(String projectUuid, DatasetRequestModel datasetRequestModel) {
        ExperimentOpsHeaders headers = HeaderUtil.getHeaders(exchange);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(datasetService.publishDatasetCreationEvent(projectUuid, datasetRequestModel, headers));
    }

    @PreAuthorize("hasAuthority('" + PermissionConstants.GET_DATASET + "')")
    @Override
    public ResponseEntity<DatasetDetailResponseModel> getDataset(String projectUuid, String uuid, Integer page, Integer size) {
        ExperimentOpsHeaders headers = HeaderUtil.getHeaders(exchange);
        return ResponseEntity.status(HttpStatus.OK).body(datasetService.getDataset(uuid, projectUuid, page, size, headers));
    }

    @PreAuthorize("hasAuthority('" + PermissionConstants.GET_DATASET + "')")
    @Override
    public ResponseEntity<DatasetListResponseModel> getDatasetList(String projectUuid, Integer page, Integer size) {
        ExperimentOpsHeaders headers = HeaderUtil.getHeaders(exchange);
        return ResponseEntity.status(HttpStatus.OK).body(datasetService.getDatasetList(projectUuid, page, size, headers));
    }

    @PreAuthorize("hasAuthority('" + PermissionConstants.EDIT_DATASET + "')")
    @Override
    public ResponseEntity<DatasetResponseModel> updateDataset(String projectUuid, String uuid, DatasetRequestModel datasetRequestModel) {
        ExperimentOpsHeaders headers = HeaderUtil.getHeaders(exchange);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(datasetService.publishDatasetUpdateEvent(uuid, projectUuid, datasetRequestModel, headers));
    }

    @PreAuthorize("hasAuthority('" + PermissionConstants.EDIT_DATASET + "')")
    @Override
    public ResponseEntity<DatasetResponseModel> updateStatusDataset(String projectUuid, String uuid, DatasetStatusChangeRequestModel datasetStatusChangeRequestModel) {
        ExperimentOpsHeaders headers = HeaderUtil.getHeaders(exchange);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(datasetService.publishDatasetStatusChangeEvent(uuid, projectUuid, datasetStatusChangeRequestModel, headers));
    }

    @PreAuthorize("hasAuthority('" + PermissionConstants.ADD_DATASET + "')")
    @Override
    public ResponseEntity<DatasetVersionResponseModel> initiateDatasetVersionUpload(String datasetUuid, DatasetVersionUploadRequestModel requestModel) {
        ExperimentOpsHeaders headers = HeaderUtil.getHeaders(exchange);
        return ResponseEntity.status(HttpStatus.OK).body(datasetService.initiateDatasetVersionUpload(datasetUuid, requestModel, headers));
    }

    @PreAuthorize("hasAuthority('" + PermissionConstants.GET_DATASET + "')")
    @Override
    public ResponseEntity<Resource> getDatasetVersion(String datasetUuid, String uuid) {
        ExperimentOpsHeaders headers = HeaderUtil.getHeaders(exchange);
        DatasetService.DatasetVersionFile datasetVersionFile = datasetService.getDatasetVersion(datasetUuid, uuid, headers);
        ByteArrayResource resource = new ByteArrayResource(datasetVersionFile.content());
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(datasetVersionFile.content().length)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(datasetVersionFile.fileName()).build().toString())
                .body(resource);
    }

    @PreAuthorize("hasAuthority('" + PermissionConstants.EDIT_DATASET + "')")
    @Override
    public ResponseEntity<DatasetVersionItemModel> updateStatusDatasetVersion(String datasetUuid, String uuid, DatasetVersionStatusChangeRequestModel datasetVersionStatusChangeRequestModel) {
        ExperimentOpsHeaders headers = HeaderUtil.getHeaders(exchange);
        datasetService.publishDatasetVersionStatusChangeEvent(datasetUuid, uuid, datasetVersionStatusChangeRequestModel, headers);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

}
