package com.experimentops.platformapi.service;

import com.experimentops.common.exceptions.runtime.EntityAlreadyExistsException;
import com.experimentops.common.exceptions.runtime.EntityNotFoundException;
import com.experimentops.common.exceptions.runtime.ValidationException;
import com.experimentops.common.kafka.KafkaProducer;
import com.experimentops.dataset.event.DatasetMutationEvent;
import com.experimentops.dataset.model.v1.DatasetDetailResponseModel;
import com.experimentops.dataset.model.v1.DatasetListResponseModel;
import com.experimentops.dataset.model.v1.DatasetRequestModel;
import com.experimentops.dataset.model.v1.DatasetResponseModel;
import com.experimentops.dataset.model.v1.DatasetStatusChangeRequestModel;
import com.experimentops.dataset.model.v1.DatasetVersionResponseModel;
import com.experimentops.dataset.model.v1.DatasetVersionStatusChangeRequestModel;
import com.experimentops.dataset.model.v1.DatasetVersionUploadRequestModel;
import com.experimentops.dataset.version.event.DatasetVersionMutationEvent;
import com.experimentops.dataset.version.scan.event.DatasetVersionScanCompletedEvent;
import com.experimentops.dataset.version.scan.event.DatasetVersionScanCompletedEventPayload;
import com.experimentops.dataset.version.scan.event.DatasetVersionScanRequestedEvent;
import com.experimentops.objectstorage.gateway.ObjectStorageGateway;
import com.experimentops.platformapi.model.entity.DatasetVersion;
import com.experimentops.platformapi.dal.repository.DatasetRepository;
import com.experimentops.platformapi.dal.repository.DatasetVersionRepository;
import com.experimentops.platformapi.model.entity.Dataset;
import com.experimentops.platformapi.model.type.DatasetScanStatusEnum;
import com.experimentops.platformapi.model.type.StatusEnum;
import com.experimentops.platformapi.transformer.DatasetTransformer;
import com.experimentops.platformapi.validator.DatasetValidator;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.ExperimentOpsUtils;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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
    private static final String DATASET_VERSION_UUID = "dataset_version_uuid";

    @Value("${dataset.topic.name}")
    private String datasetTopic;

    @Value("${dataset.version.topic.name}")
    private String datasetVersionTopic;

    @Value("${dataset.version.scan.requested.topic}")
    private String datasetVersionScanRequestedTopic;

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

    public void publishDatasetStatusChangeEvent(@NonNull String uuid, @NonNull String projectUuid, @NonNull DatasetStatusChangeRequestModel statusChangeRequestModel, @NonNull ExperimentOpsHeaders headers) {
        log.info(headers, "changing status of dataset with uuid " + uuid);
        datasetRepository
                .findByUuidAndProjectUuidAndWorkspaceUuidAndEnabled(uuid, projectUuid, headers.getWorkspaceUuid(), true)
                .orElseThrow(() -> new EntityNotFoundException(DATASET_UUID, uuid));
        DatasetMutationEvent datasetMutationEvent = datasetTransformer.transformDatasetStatusChangeEvent(uuid, projectUuid, statusChangeRequestModel, headers);
        kafkaProducer.sendMessage(datasetTopic, datasetMutationEvent, datasetMutationEvent.getMetadata());
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
    public DatasetVersionResponseModel initiateDatasetVersionUpload(@NonNull String datasetUuid, @NonNull DatasetVersionUploadRequestModel requestModel, @NonNull ExperimentOpsHeaders headers) {
        log.info(headers, "initiating dataset version upload for dataset uuid: " + datasetUuid);
        datasetValidator.validateDatasetVersionUploadRequest(requestModel);
        Dataset dataset = datasetRepository
                .findByUuidAndWorkspaceUuidAndStatusAndEnabled(datasetUuid, headers.getWorkspaceUuid(), StatusEnum.ACTIVE, true)
                .orElseThrow(() -> new EntityNotFoundException(DATASET_UUID, datasetUuid));
        String versionUuid = ExperimentOpsUtils.uuid();
        ObjectStorageGateway.PresignedDatasetUpload upload = objectStorageGateway.createPresignedDatasetUpload(
                headers.getWorkspaceUuid(), dataset.getProjectUuid(), datasetUuid, versionUuid, requestModel.getFileName(), headers
        );

        DatasetVersionMutationEvent datasetVersionMutationEvent = datasetTransformer.transformDatasetVersionCreationEvent(
                versionUuid, datasetUuid, requestModel.getFileName(), upload.storageUri(), null, null, headers
        );

        kafkaProducer.sendMessage(datasetVersionTopic, datasetVersionMutationEvent, datasetVersionMutationEvent.getMetadata());

        return datasetTransformer.transformDatasetVersionResponseModel(versionUuid, upload, headers);
    }

    public void createDatasetVersion(@NonNull DatasetVersionMutationEvent event, @NonNull ExperimentOpsHeaders headers) {
        DatasetVersion datasetVersion = datasetTransformer.transformDatasetVersionEntity(event, headers);
        datasetVersionRepository.save(datasetVersion);
        log.info(headers, "saved dataset version with uuid: " + datasetVersion.getUuid());
    }

    public void publishDatasetVersionStatusChangeEvent(@NonNull String datasetUuid, @NonNull String uuid, @NonNull DatasetVersionStatusChangeRequestModel requestModel, @NonNull ExperimentOpsHeaders headers) {
        log.info(headers, "publishing status change for dataset version with uuid " + uuid);
        StatusEnum status = datasetValidator.validateDatasetVersionStatusChangeRequestModel(requestModel);
        validateDatasetExists(datasetUuid, headers);
        DatasetVersion datasetVersion = datasetVersionRepository
                .findByUuidAndDatasetUuidAndWorkspaceUuidAndEnabled(uuid, datasetUuid, headers.getWorkspaceUuid(), true)
                .orElseThrow(() -> new EntityNotFoundException(DATASET_VERSION_UUID, uuid));

        Long size = null;
        String format = null;
        if (StatusEnum.ACTIVE == status) {
            ObjectStorageGateway.DatasetFileMetadata metadata = objectStorageGateway
                    .getDatasetFileMetadata(toObjectKey(datasetVersion.getStorageUri()));
            size = metadata.size();
            format = datasetFileFormatDetector.detect(datasetVersion.getName(), metadata.contentType()).name();
        } else if (StatusEnum.FAILED == status) {
            log.error(headers, "Dataset version scan failed for uuid " + uuid + ": " + requestModel.getFailureMessage());
        }

        DatasetVersionMutationEvent event = datasetTransformer.transformDatasetVersionStatusChangeEvent(
                uuid, datasetUuid, status, size, format, requestModel.getFailureMessage(), headers
        );
        kafkaProducer.sendMessage(datasetVersionTopic, event, event.getMetadata());
    }

    public void changeStatusDatasetVersion(@NonNull DatasetVersionMutationEvent event, @NonNull ExperimentOpsHeaders headers) {
        StatusEnum status = StatusEnum.of(event.getPayload().getStatus());
        DatasetVersion datasetVersion = datasetVersionRepository
                .findByUuidAndDatasetUuidAndWorkspaceUuidAndEnabled(
                        event.getMetadata().getUuid(),
                        event.getPayload().getDatasetUuid(),
                        headers.getWorkspaceUuid(),
                        true)
                .orElseThrow(() -> new EntityNotFoundException(DATASET_VERSION_UUID, event.getMetadata().getUuid()));
        datasetVersion.setStatus(status);
        if (event.getPayload().getSize() != null) datasetVersion.setSize(event.getPayload().getSize());
        if (event.getPayload().getFormat() != null) datasetVersion.setFormat(event.getPayload().getFormat());
        if (StatusEnum.ACTIVE == status) {
            datasetVersion.setScanStatus(DatasetScanStatusEnum.PENDING);
            datasetVersion.setScanMessage(null);
        }
        datasetVersion.setUpdatedBy(headers.getUserUuid());
        datasetVersion = datasetVersionRepository.save(datasetVersion);
        if (StatusEnum.ACTIVE == status) {
            DatasetVersionScanRequestedEvent scanRequestedEvent = datasetTransformer.transformDatasetVersionScanRequestedEvent(datasetVersion, headers);
            kafkaProducer.sendMessage(datasetVersionScanRequestedTopic, scanRequestedEvent, scanRequestedEvent.getMetadata());
        }
    }

    public void processDatasetVersionScanCompleted(@NonNull DatasetVersionScanCompletedEvent event, @NonNull ExperimentOpsHeaders headers) {
        DatasetVersionScanCompletedEventPayload eventPayload = event.getPayload();
        DatasetScanStatusEnum scanStatus = StringUtils.isBlank(eventPayload.getMessage()) ? DatasetScanStatusEnum.COMPLETED : DatasetScanStatusEnum.FAILED;
        DatasetVersion datasetVersion = datasetVersionRepository
                .findByUuidAndWorkspaceUuidAndEnabled(event.getMetadata().getUuid(), headers.getWorkspaceUuid(), true)
                .orElseThrow(() -> new EntityNotFoundException(DATASET_VERSION_UUID, event.getMetadata().getUuid()));
        datasetVersion.setScanStatus(scanStatus);
        datasetVersion.setScanMessage(eventPayload.getMessage());
        datasetVersion.setPreviewUri(eventPayload.getPreviewUri());
        datasetVersion.setUpdatedBy(headers.getUserUuid());
        datasetVersionRepository.save(datasetVersion);
        if (DatasetScanStatusEnum.FAILED == scanStatus) {
            log.error(headers, "Dataset version scan failed for uuid " + event.getMetadata().getUuid() + ": " + eventPayload.getMessage());
        }
    }

    @NonNull
    public DatasetVersionFile getDatasetVersion(@NonNull String datasetUuid, @NonNull String uuid, @NonNull ExperimentOpsHeaders headers) {
        log.info(headers, "getting dataset version with uuid " + uuid);
        validateDatasetExists(datasetUuid, headers);
        DatasetVersion datasetVersion = findActiveDatasetVersion(datasetUuid, uuid, headers);
        if (StringUtils.isBlank(datasetVersion.getPreviewUri())){
            throw new EntityNotFoundException("Preview not available for uuid " + uuid);
        }
        byte[] content = objectStorageGateway.downloadFile(toObjectKey(datasetVersion.getPreviewUri()));
        return new DatasetVersionFile(datasetVersion.getName(), content);
    }

    @NonNull
    public DatasetDetailResponseModel getDataset(@NonNull String uuid, @NonNull String projectUuid, Integer page, Integer size, @NonNull ExperimentOpsHeaders headers) {
        log.info(headers, "getting dataset with uuid " + uuid);
        Dataset dataset = datasetRepository
                .findByUuidAndProjectUuidAndWorkspaceUuidAndStatusAndEnabled(uuid, projectUuid, headers.getWorkspaceUuid(), StatusEnum.ACTIVE, true)
                .orElseThrow(() -> new EntityNotFoundException(DATASET_UUID, uuid));
        Pageable pageable = PaginationUtil.createPageRequest(page, size);
        Page<DatasetVersion> datasetVersionPage = datasetVersionRepository
                .findAllByDatasetUuidAndWorkspaceUuidAndStatusAndEnabledOrderByCreationDateDesc(
                        dataset.getUuid(),
                        headers.getWorkspaceUuid(),
                        StatusEnum.ACTIVE,
                        true,
                        pageable);
        return datasetTransformer.transformDatasetDetailResponseModel(dataset, datasetVersionPage.getContent(), datasetVersionPage.getTotalElements(), headers);
    }

    @NonNull
    public DatasetListResponseModel getDatasetList(@NonNull String projectUuid, Integer page, Integer size, @NonNull ExperimentOpsHeaders headers) {
        log.info(headers, "getting dataset list for project uuid " + projectUuid);
        Pageable pageable = PaginationUtil.createPageRequest(page, size);
        Page<Dataset> datasetsPage = datasetRepository
                .findAllByProjectUuidAndWorkspaceUuidAndStatusAndEnabledOrderByLastUpdatedDesc(projectUuid, headers.getWorkspaceUuid(), StatusEnum.ACTIVE, true, pageable);
        List<DatasetResponseModel> datasets = datasetsPage
                .getContent()
                .stream()
                .map(dataset -> {
                    int versionCount = (int) datasetVersionRepository.countByDatasetUuidAndWorkspaceUuidAndStatusAndEnabled(dataset.getUuid(), headers.getWorkspaceUuid(), StatusEnum.ACTIVE, true);
                    return datasetTransformer.transformDatasetResponseModelFromEntity(dataset, versionCount, headers);
                })
                .toList();
        DatasetListResponseModel response = new DatasetListResponseModel();
        response.setData(datasets);
        response.setTotalElements(datasetsPage.getTotalElements());
        return response;
    }

    private void validateDatasetExists(@NonNull String datasetUuid, @NonNull ExperimentOpsHeaders headers) {
        datasetRepository
                .findByUuidAndWorkspaceUuidAndStatusAndEnabled(datasetUuid, headers.getWorkspaceUuid(), StatusEnum.ACTIVE, true)
                .orElseThrow(() -> new EntityNotFoundException(DATASET_UUID, datasetUuid));
    }

    @NonNull
    private DatasetVersion findActiveDatasetVersion(@NonNull String datasetUuid, @NonNull String uuid, @NonNull ExperimentOpsHeaders headers) {
        return datasetVersionRepository
                .findByUuidAndDatasetUuidAndWorkspaceUuidAndStatusAndEnabled(
                        uuid,
                        datasetUuid,
                        headers.getWorkspaceUuid(),
                        StatusEnum.ACTIVE,
                        true)
                .orElseThrow(() -> new EntityNotFoundException(DATASET_VERSION_UUID, uuid));
    }

    @NonNull
    private String toObjectKey(String storageUri) {
        if (storageUri == null || storageUri.isBlank()) {
            throw new ValidationException(INVALID_INPUTS, "Dataset version storage uri is missing");
        }
        if (!storageUri.startsWith("s3://")) {
            return storageUri;
        }
        int objectKeyStart = storageUri.indexOf('/', "s3://".length());
        if (objectKeyStart < 0 || objectKeyStart == storageUri.length() - 1) {
            throw new ValidationException(INVALID_INPUTS, "Dataset version storage uri is invalid");
        }
        return storageUri.substring(objectKeyStart + 1);
    }

    public record DatasetVersionFile(String fileName, byte[] content) {
    }

}
