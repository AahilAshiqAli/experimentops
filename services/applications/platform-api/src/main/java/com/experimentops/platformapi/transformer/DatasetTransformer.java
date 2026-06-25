package com.experimentops.platformapi.transformer;

import com.experimentops.avroevent.type.EventType;
import com.experimentops.common.kafka.model.event.ExperimentOpsMetadataEvent;
import com.experimentops.common.kafka.utils.ExperimentOpsMetadataUtil;
import com.experimentops.dataset.event.DatasetMutationEvent;
import com.experimentops.dataset.event.DatasetMutationEventPayload;
import com.experimentops.dataset.model.v1.DatasetDetailResponseModel;
import com.experimentops.dataset.model.v1.DatasetRequestModel;
import com.experimentops.dataset.model.v1.DatasetResponseModel;
import com.experimentops.dataset.model.v1.DatasetStatusChangeRequestModel;
import com.experimentops.dataset.model.v1.DatasetVersionItemModel;
import com.experimentops.dataset.version.event.DatasetVersionMutationEvent;
import com.experimentops.dataset.version.event.DatasetVersionMutationEventPayload;
import com.experimentops.platformapi.model.entity.Dataset;
import com.experimentops.platformapi.model.entity.DatasetVersion;
import com.experimentops.platformapi.model.type.StatusEnum;

import java.util.List;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.ExperimentOpsUtils;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

@Component
public class DatasetTransformer {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(DatasetTransformer.class);

    public DatasetMutationEvent transformDatasetCreationEvent(@NonNull String projectUuid, @NonNull DatasetRequestModel requestModel, @NonNull ExperimentOpsHeaders headers) {

        log.info(headers, "transforming the payload to dataset mutation event");

        DatasetMutationEventPayload payload = DatasetMutationEventPayload.newBuilder()
                .setDatasetName(requestModel.getName())
                .setProjectUuid(projectUuid)
                .build();

        ExperimentOpsMetadataEvent metadata = ExperimentOpsMetadataUtil.metadataEvent(
                headers,
                ExperimentOpsUtils.uuid(),
                EventType.DATASET_CREATE.name(),
                this.getClass().getSimpleName()
        );

        return DatasetMutationEvent.newBuilder()
                .setMetadata(metadata)
                .setPayload(payload)
                .build();
    }

    public DatasetMutationEvent transformDatasetUpdateEvent(@NonNull String uuid, @NonNull String projectUuid, @NonNull DatasetRequestModel requestModel, @NonNull ExperimentOpsHeaders headers) {

        log.info(headers, "transforming the payload to dataset mutation event for update");

        DatasetMutationEventPayload payload = DatasetMutationEventPayload.newBuilder()
                .setDatasetName(requestModel.getName())
                .setProjectUuid(projectUuid)
                .build();

        ExperimentOpsMetadataEvent metadata = ExperimentOpsMetadataUtil.metadataEvent(
                headers,
                uuid,
                EventType.DATASET_UPDATE.name(),
                this.getClass().getSimpleName()
        );

        return DatasetMutationEvent.newBuilder()
                .setMetadata(metadata)
                .setPayload(payload)
                .build();
    }

    public DatasetMutationEvent transformDatasetStatusChangeEvent(@NonNull String uuid, @NonNull String projectUuid, @NonNull DatasetStatusChangeRequestModel statusChangeRequestModel, @NonNull ExperimentOpsHeaders headers) {

        log.info(headers, "transforming the payload to dataset mutation event for status change");

        DatasetMutationEventPayload payload = DatasetMutationEventPayload.newBuilder()
                .setProjectUuid(projectUuid)
                .setStatus(statusChangeRequestModel.getStatus())
                .build();

        ExperimentOpsMetadataEvent metadata = ExperimentOpsMetadataUtil.metadataEvent(
                headers,
                uuid,
                EventType.DATASET_STATUS_CHANGE.name(),
                this.getClass().getSimpleName()
        );

        return DatasetMutationEvent.newBuilder()
                .setMetadata(metadata)
                .setPayload(payload)
                .build();
    }

    public DatasetResponseModel transformDatasetResponseModel(@NonNull DatasetMutationEvent event, @NonNull ExperimentOpsHeaders headers) {

        log.info(headers, "transforming the payload to Dataset Response Model");

        DatasetMutationEventPayload payload = event.getPayload();

        DatasetResponseModel responseModel = new DatasetResponseModel();
        responseModel.setDatasetUuid(event.getMetadata().getUuid());
        responseModel.setName(payload.getDatasetName());
        responseModel.setProjectUuid(payload.getProjectUuid());

        return responseModel;
    }

    @NonNull
    public DatasetResponseModel transformDatasetResponseModelFromEntity(@NonNull Dataset dataset, int versionCount, @NonNull ExperimentOpsHeaders headers) {

        log.info(headers, "transforming the Dataset entity to Dataset Response Model");

        DatasetResponseModel responseModel = new DatasetResponseModel();
        responseModel.setDatasetUuid(dataset.getUuid());
        responseModel.setName(dataset.getName());
        responseModel.setProjectUuid(dataset.getProjectUuid());
        responseModel.setStatus(dataset.getStatus().name());
        responseModel.setVersionCount(versionCount);
        responseModel.setUpdatedAt(dataset.getLastUpdated().toLocalDateTime().toString());

        return responseModel;
    }

    @NonNull
    public DatasetDetailResponseModel transformDatasetDetailResponseModel(@NonNull Dataset dataset, @NonNull List<DatasetVersion> versions, @NonNull ExperimentOpsHeaders headers) {

        log.info(headers, "transforming the Dataset entity to Dataset Detail Response Model");

        List<DatasetVersionItemModel> versionItems = versions.stream()
                .map(v -> {
                    DatasetVersionItemModel item = new DatasetVersionItemModel();
                    item.setDatasetVersionUuid(v.getUuid());
                    item.setOriginalFileName(v.getName());
                    item.setFormat(DatasetVersionItemModel.FormatEnum.fromValue(normalizeDatasetFormat(v.getFormat())));
                    item.setSize(v.getSize());
                    item.setUpdatedAt(v.getLastUpdated().toLocalDateTime().toString());
                    return item;
                })
                .toList();

        DatasetDetailResponseModel responseModel = new DatasetDetailResponseModel();
        responseModel.setDatasetUuid(dataset.getUuid());
        responseModel.setProjectUuid(dataset.getProjectUuid());
        responseModel.setName(dataset.getName());
        responseModel.setVersions(versionItems);

        return responseModel;
    }

    @NonNull
    public Dataset transformDatasetEntity(@NonNull DatasetMutationEvent event, @NonNull ExperimentOpsHeaders headers) {

        log.info(headers, "transforming the payload to Dataset Entity");

        DatasetMutationEventPayload payload = event.getPayload();
        Dataset dataset = Dataset.builder()
                .name(payload.getDatasetName())
                .projectUuid(payload.getProjectUuid())
                .workspaceUuid(event.getMetadata().getWorkspaceUuid())
                .status(StatusEnum.ACTIVE)
                .build();
        dataset.setUuid(event.getMetadata().getUuid());

        return dataset;
    }

    @NonNull
    public DatasetVersion transformDatasetVersionEntity(@NonNull DatasetVersionMutationEvent event, @NonNull ExperimentOpsHeaders headers) {

        log.info(headers, "transforming the payload to DatasetVersion Entity");

        DatasetVersionMutationEventPayload payload = event.getPayload();
        DatasetVersion datasetVersion = DatasetVersion.builder()
                .name(payload.getFileName())
                .storageUri(payload.getStorageUri())
                .format(normalizeDatasetFormat(payload.getFormat()))
                .size(payload.getSize())
                .datasetUuid(payload.getDatasetUuid())
                .workspaceUuid(event.getMetadata().getWorkspaceUuid())
                .status(StatusEnum.ACTIVE)
                .build();
        datasetVersion.setUuid(event.getMetadata().getUuid());

        return datasetVersion;
    }

    @NonNull
    public DatasetVersionMutationEvent transformDatasetVersionCreationEvent(
            @NonNull String uuid,
            @NonNull String datasetUuid,
            @NonNull String fileName,
            @NonNull String storageUri,
            String format,
            long size,
            @NonNull ExperimentOpsHeaders headers){

        DatasetVersionMutationEventPayload payload = DatasetVersionMutationEventPayload.newBuilder()
                .setDatasetUuid(datasetUuid)
                .setStorageUri(storageUri)
                .setFileName(fileName)
                .setFormat(format)
                .setSize(size)
                .build();

        ExperimentOpsMetadataEvent metadata = ExperimentOpsMetadataUtil.metadataEvent(
                headers,
                uuid,
                EventType.DATASET_VERSION_CREATE.name(),
                this.getClass().getSimpleName()
        );

        return DatasetVersionMutationEvent.newBuilder()
                .setMetadata(metadata)
                .setPayload(payload)
                .build();

    }

    private String normalizeDatasetFormat(String format) {
        if (format == null || format.isBlank()) {
            return "UNKNOWN";
        }

        String normalizedFormat = format.trim().toUpperCase();
        return switch (normalizedFormat) {
            case "TEXT/CSV", "APPLICATION/CSV" -> "CSV";
            case "APPLICATION/VND.MS-EXCEL",
                 "APPLICATION/VND.OPENXMLFORMATS-OFFICEDOCUMENT.SPREADSHEETML.SHEET" -> "EXCEL";
            case "TEXT/PLAIN" -> "TEXT";
            case "CSV", "EXCEL", "TEXT" -> normalizedFormat;
            default -> {
                yield "UNKNOWN";
            }
        };
    }

}
