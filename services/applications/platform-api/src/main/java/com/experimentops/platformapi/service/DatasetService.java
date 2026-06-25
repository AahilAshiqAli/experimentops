package com.experimentops.platformapi.service;

import com.experimentops.common.exceptions.runtime.EntityAlreadyExistsException;
import com.experimentops.common.exceptions.runtime.EntityNotFoundException;
import com.experimentops.common.exceptions.runtime.ValidationException;
import com.experimentops.common.kafka.KafkaProducer;
import com.experimentops.dataset.event.DatasetMutationEvent;
import com.experimentops.dataset.model.v1.DatasetDetailResponseModel;
import com.experimentops.dataset.model.v1.DatasetRequestModel;
import com.experimentops.dataset.model.v1.DatasetResponseModel;
import com.experimentops.dataset.model.v1.DatasetStatusChangeRequestModel;
import com.experimentops.dataset.model.v1.DatasetVersionResponseModel;
import com.experimentops.dataset.version.event.DatasetVersionMutationEvent;
import com.experimentops.platformapi.model.entity.DatasetVersion;
import com.experimentops.platformapi.dal.gateway.ObjectStorageGateway;
import com.experimentops.platformapi.dal.gateway.dto.UploadedObject;
import com.experimentops.platformapi.dal.repository.DatasetRepository;
import com.experimentops.platformapi.dal.repository.DatasetVersionRepository;
import com.experimentops.platformapi.model.entity.Dataset;
import com.experimentops.platformapi.model.type.DatasetFileFormatEnum;
import com.experimentops.platformapi.model.type.StatusEnum;
import com.experimentops.platformapi.transformer.DatasetTransformer;
import com.experimentops.platformapi.validator.DatasetValidator;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.ExperimentOpsUtils;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static com.experimentops.common.exceptions.constant.ErrorCode.INVALID_INPUTS;


@Service
@RequiredArgsConstructor
public class DatasetService {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(DatasetService.class);

    private final DatasetValidator datasetValidator;
    private final DatasetRepository datasetRepository;
    private final DatasetVersionRepository datasetVersionRepository;
    private final DatasetTransformer datasetTransformer;
    private final KafkaProducer kafkaProducer;
    private final ObjectStorageGateway objectStorageGateway;
    private final DatasetFileFormatDetector datasetFileFormatDetector;

    private static final String DATASET_UUID = "dataset_uuid";

    @Value("${dataset.topic.name}")
    private String datasetTopic;

    @Value("${dataset.version.topic.name}")
    private String datasetVersionTopic;

    @NonNull
    public DatasetResponseModel publishDatasetCreationEvent(@NonNull String projectUuid, @NonNull DatasetRequestModel requestModel, @NonNull ExperimentOpsHeaders headers) {
        log.info(headers, "Creating new dataset with name " + requestModel.getName());
        datasetValidator.validateDatasetRequestModel(requestModel);
        datasetRepository
                .findByNameAndProjectUuidAndWorkspaceUuidAndStatusAndEnabled(
                        requestModel.getName(),
                        projectUuid,
                        headers.getWorkspaceUuid(),
                        StatusEnum.ACTIVE,
                        true)
                .ifPresent(dataset -> {
                    throw new EntityAlreadyExistsException("name", requestModel.getName());
                });
        DatasetMutationEvent datasetMutationEvent = datasetTransformer.transformDatasetCreationEvent(projectUuid, requestModel, headers);
        kafkaProducer.sendMessage(datasetTopic, datasetMutationEvent, datasetMutationEvent.getMetadata());
        return datasetTransformer.transformDatasetResponseModel(datasetMutationEvent, headers);
    }

    public void createDataset(@NonNull DatasetMutationEvent datasetMutationEvent, @NonNull ExperimentOpsHeaders headers) {
        Dataset dataset = datasetTransformer.transformDatasetEntity(datasetMutationEvent, headers);
        datasetRepository.save(dataset);
        log.info(headers, "saved dataset with dataset uuid: " + dataset.getUuid());
    }

    @NonNull
    public DatasetResponseModel publishDatasetUpdateEvent(@NonNull String uuid, @NonNull String projectUuid, @NonNull DatasetRequestModel requestModel, @NonNull ExperimentOpsHeaders headers) {
        log.info(headers, "updating dataset with uuid " + uuid);
        datasetValidator.validateDatasetRequestModel(requestModel);
        datasetRepository
                .findByUuidAndProjectUuidAndWorkspaceUuidAndStatusAndEnabled(uuid, projectUuid, headers.getWorkspaceUuid(), StatusEnum.ACTIVE, true)
                .orElseThrow(() -> new EntityNotFoundException(DATASET_UUID, uuid));
        DatasetMutationEvent datasetMutationEvent = datasetTransformer.transformDatasetUpdateEvent(uuid, projectUuid, requestModel, headers);
        kafkaProducer.sendMessage(datasetTopic, datasetMutationEvent, datasetMutationEvent.getMetadata());
        return datasetTransformer.transformDatasetResponseModel(datasetMutationEvent, headers);
    }

    public void updateDataset(@NonNull DatasetMutationEvent datasetMutationEvent, @NonNull ExperimentOpsHeaders headers) {
        String datasetUuid = datasetMutationEvent.getMetadata().getUuid();
        String projectUuid = datasetMutationEvent.getPayload().getProjectUuid();
        datasetRepository
                .findByUuidAndProjectUuidAndWorkspaceUuidAndStatusAndEnabled(datasetUuid, projectUuid, headers.getWorkspaceUuid(), StatusEnum.ACTIVE, true)
                .ifPresentOrElse(
                        dataset -> {
                            dataset.setName(datasetMutationEvent.getPayload().getDatasetName());
                            datasetRepository.save(dataset);
                        },
                        () -> {
                            throw new EntityNotFoundException(DATASET_UUID, datasetUuid);
                        }
                );
        log.info(headers, "dataset update completed");
    }

    @NonNull
    public DatasetResponseModel publishDatasetStatusChangeEvent(@NonNull String uuid, @NonNull String projectUuid, @NonNull DatasetStatusChangeRequestModel statusChangeRequestModel, @NonNull ExperimentOpsHeaders headers) {
        log.info(headers, "changing status of dataset with uuid " + uuid);
        Dataset dataset = datasetRepository
                .findByUuidAndProjectUuidAndWorkspaceUuidAndEnabled(uuid, projectUuid, headers.getWorkspaceUuid(), true)
                .orElseThrow(() -> new EntityNotFoundException(DATASET_UUID, uuid));
        DatasetMutationEvent datasetMutationEvent = datasetTransformer.transformDatasetStatusChangeEvent(uuid, projectUuid, statusChangeRequestModel, headers);
        kafkaProducer.sendMessage(datasetTopic, datasetMutationEvent, datasetMutationEvent.getMetadata());
        int versionCount = (int) datasetVersionRepository.countByDatasetUuidAndWorkspaceUuidAndEnabled(dataset.getUuid(), headers.getWorkspaceUuid(), true);
        return datasetTransformer.transformDatasetResponseModelFromEntity(dataset, versionCount, headers);
    }

    public void changeStatusDataset(@NonNull DatasetMutationEvent datasetMutationEvent, @NonNull ExperimentOpsHeaders headers) {
        String datasetUuid = datasetMutationEvent.getMetadata().getUuid();
        String projectUuid = datasetMutationEvent.getPayload().getProjectUuid();
        StatusEnum newStatus = StatusEnum.valueOf(datasetMutationEvent.getPayload().getStatus());
        datasetRepository
                .findByUuidAndProjectUuidAndWorkspaceUuidAndEnabled(datasetUuid, projectUuid, headers.getWorkspaceUuid(), true)
                .ifPresentOrElse(
                        dataset -> {
                            dataset.setStatus(newStatus);
                            datasetRepository.save(dataset);
                        },
                        () -> {
                            throw new EntityNotFoundException(DATASET_UUID, datasetUuid);
                        }
                );
        log.info(headers, "dataset status change completed");
    }

    @NonNull
    public DatasetVersionResponseModel publishUploadDatasetVersion(@NonNull String datasetUuid, @NonNull MultipartFile file, @NonNull ExperimentOpsHeaders headers) {
        log.info(headers, "uploading dataset version for dataset uuid: " + datasetUuid);
        DatasetFileFormatEnum datasetFileFormat = validateDatasetVersionFile(file);
        Dataset dataset = datasetRepository
                .findByUuidAndWorkspaceUuidAndStatusAndEnabled(datasetUuid, headers.getWorkspaceUuid(), StatusEnum.ACTIVE, true)
                .orElseThrow(() -> new EntityNotFoundException(DATASET_UUID, datasetUuid));
        String versionUuid = ExperimentOpsUtils.uuid();
        UploadedObject uploadedObject = objectStorageGateway.uploadDatasetFile(headers.getWorkspaceUuid(), dataset.getProjectUuid(), versionUuid, file, headers);
        log.info(headers, uploadedObject.toString());
        long countVersions = datasetVersionRepository.countByDatasetUuidAndWorkspaceUuidAndEnabled(datasetUuid, headers.getWorkspaceUuid(), true);
        String fileName = "v" + (countVersions + 1) + "_" + uploadedObject.originalFilename();
        DatasetVersionMutationEvent datasetVersionMutationEvent = datasetTransformer.transformDatasetVersionCreationEvent(
                versionUuid,
                datasetUuid,
                fileName,
                uploadedObject.storageUri(),
                datasetFileFormat.name(),
                uploadedObject.sizeBytes(),
                headers
        );
        kafkaProducer.sendMessage(datasetVersionTopic, datasetVersionMutationEvent, datasetVersionMutationEvent.getMetadata() );
        DatasetVersionResponseModel responseModel = new DatasetVersionResponseModel();
        responseModel.setUuid(versionUuid);
        return responseModel;
    }

    public void createDatasetVersion(@NonNull DatasetVersionMutationEvent event, @NonNull ExperimentOpsHeaders headers) {
        DatasetVersion datasetVersion = datasetTransformer.transformDatasetVersionEntity(event, headers);
        datasetVersionRepository.save(datasetVersion);
        log.info(headers, "saved dataset version with uuid: " + datasetVersion.getUuid());
    }

    @NonNull
    public DatasetDetailResponseModel getDataset(@NonNull String uuid, @NonNull String projectUuid, @NonNull ExperimentOpsHeaders headers) {
        log.info(headers, "getting dataset with uuid " + uuid);
        Dataset dataset = datasetRepository
                .findByUuidAndProjectUuidAndWorkspaceUuidAndStatusAndEnabled(uuid, projectUuid, headers.getWorkspaceUuid(), StatusEnum.ACTIVE, true)
                .orElseThrow(() -> new EntityNotFoundException(DATASET_UUID, uuid));
        List<DatasetVersion> versions = datasetVersionRepository.findAllByDatasetUuidAndWorkspaceUuidAndEnabledOrderByCreationDateDesc(dataset.getUuid(), headers.getWorkspaceUuid(), true);
        return datasetTransformer.transformDatasetDetailResponseModel(dataset, versions, headers);
    }

    @NonNull
    public List<DatasetResponseModel> getDatasetList(@NonNull String projectUuid, @NonNull ExperimentOpsHeaders headers) {
        log.info(headers, "getting dataset list for project uuid " + projectUuid);
        return datasetRepository
                .findAllByProjectUuidAndWorkspaceUuidAndStatusAndEnabledOrderByLastUpdatedDesc(projectUuid, headers.getWorkspaceUuid(), StatusEnum.ACTIVE, true)
                .stream()
                .map(dataset -> {
                    int versionCount = (int) datasetVersionRepository.countByDatasetUuidAndWorkspaceUuidAndEnabled(dataset.getUuid(), headers.getWorkspaceUuid(), true);
                    return datasetTransformer.transformDatasetResponseModelFromEntity(dataset, versionCount, headers);
                })
                .toList();
    }

    @NonNull
    private DatasetFileFormatEnum validateDatasetVersionFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException(INVALID_INPUTS, "Dataset file is required");
        }

        DatasetFileFormatEnum datasetFileFormat = datasetFileFormatDetector.detect(file.getOriginalFilename(), file.getContentType());
        if (DatasetFileFormatEnum.UNKNOWN == datasetFileFormat) {
            throw new ValidationException(INVALID_INPUTS, "Unsupported dataset file format. Supported formats are CSV, EXCEL, TEXT");
        }

        return datasetFileFormat;
    }

}
