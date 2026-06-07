package com.experimentops.platformapi.service;

import com.experimentops.dataset.model.v1.DatasetResponseModel;
import com.experimentops.platformapi.dal.gateway.ObjectStorageGateway;
import com.experimentops.platformapi.dal.gateway.dto.UploadedObject;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.ExperimentOpsUtils;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class DatasetService {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(DatasetService.class);

    private final ObjectStorageGateway objectStorageGateway;

    public DatasetResponseModel uploadDataset(String projectUuid, MultipartFile file, ExperimentOpsHeaders headers){
        log.info(headers, "uploading dataset for workspace: " + headers.getWorkspaceUuid());
        String datasetUuid = ExperimentOpsUtils.uuid();
        UploadedObject uploadDatasetFile = objectStorageGateway.uploadDatasetFile(headers.getWorkspaceUuid(), projectUuid, datasetUuid, file, headers);
        log.info(headers, uploadDatasetFile.toString());
        DatasetResponseModel datasetResponseModel = new DatasetResponseModel();
        datasetResponseModel.uuid(datasetUuid);
        return datasetResponseModel;
    }
}
