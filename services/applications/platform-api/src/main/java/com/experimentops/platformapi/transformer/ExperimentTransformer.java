package com.experimentops.platformapi.transformer;

import com.experimentops.avroevent.type.EventType;
import com.experimentops.common.kafka.model.event.ExperimentOpsMetadataEvent;
import com.experimentops.common.kafka.utils.ExperimentOpsMetadataUtil;
import com.experimentops.experiment.event.ExperimentMutationEvent;
import com.experimentops.experiment.event.ExperimentMutationEventPayload;
import com.experimentops.experiment.model.v1.ExperimentListItemModel;
import com.experimentops.experiment.model.v1.ExperimentRequestModel;
import com.experimentops.experiment.model.v1.ExperimentResponseModel;
import com.experimentops.experiment.model.v1.ExperimentStatusChangeRequestModel;
import com.experimentops.platformapi.model.entity.Experiment;
import com.experimentops.platformapi.model.type.StatusEnum;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.ExperimentOpsUtils;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

@Component
public class ExperimentTransformer {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(ExperimentTransformer.class);

    public ExperimentMutationEvent transformExperimentCreationEvent(@NonNull String projectUuid, @NonNull ExperimentRequestModel requestModel, @NonNull ExperimentOpsHeaders headers) {

        log.info(headers, "transforming the payload to experiment mutation event");

        ExperimentMutationEventPayload payload = ExperimentMutationEventPayload.newBuilder()
                .setExperimentName(requestModel.getName())
                .setDescription(requestModel.getDescription())
                .setExperimentType(requestModel.getExperimentType())
                .setProjectUuid(projectUuid)
                .build();

        ExperimentOpsMetadataEvent metadata = ExperimentOpsMetadataUtil.metadataEvent(
                headers,
                ExperimentOpsUtils.uuid(),
                EventType.EXPERIMENT_CREATE.name(),
                this.getClass().getSimpleName()
        );

        return ExperimentMutationEvent.newBuilder()
                .setMetadata(metadata)
                .setPayload(payload)
                .build();
    }

    public ExperimentMutationEvent transformExperimentUpdateEvent(@NonNull String uuid, @NonNull String projectUuid, @NonNull ExperimentRequestModel requestModel, @NonNull ExperimentOpsHeaders headers) {

        log.info(headers, "transforming the payload to experiment mutation event for update");

        ExperimentMutationEventPayload payload = ExperimentMutationEventPayload.newBuilder()
                .setExperimentName(requestModel.getName())
                .setDescription(requestModel.getDescription())
                .setExperimentType(requestModel.getExperimentType())
                .setProjectUuid(projectUuid)
                .build();

        ExperimentOpsMetadataEvent metadata = ExperimentOpsMetadataUtil.metadataEvent(
                headers,
                uuid,
                EventType.EXPERIMENT_UPDATE.name(),
                this.getClass().getSimpleName()
        );

        return ExperimentMutationEvent.newBuilder()
                .setMetadata(metadata)
                .setPayload(payload)
                .build();
    }

    public ExperimentMutationEvent transformExperimentStatusChangeEvent(@NonNull String uuid, @NonNull String projectUuid, @NonNull ExperimentStatusChangeRequestModel statusChangeRequestModel, @NonNull ExperimentOpsHeaders headers) {

        log.info(headers, "transforming the payload to experiment mutation event for status change");

        ExperimentMutationEventPayload payload = ExperimentMutationEventPayload.newBuilder()
                .setProjectUuid(projectUuid)
                .setStatus(statusChangeRequestModel.getStatus())
                .build();

        ExperimentOpsMetadataEvent metadata = ExperimentOpsMetadataUtil.metadataEvent(
                headers,
                uuid,
                EventType.EXPERIMENT_STATUS_CHANGE.name(),
                this.getClass().getSimpleName()
        );

        return ExperimentMutationEvent.newBuilder()
                .setMetadata(metadata)
                .setPayload(payload)
                .build();
    }

    public ExperimentResponseModel transformExperimentResponseModel(@NonNull ExperimentMutationEvent event, @NonNull ExperimentOpsHeaders headers) {

        log.info(headers, "transforming the payload to Experiment Response Model");

        ExperimentMutationEventPayload payload = event.getPayload();

        ExperimentResponseModel responseModel = new ExperimentResponseModel();
        responseModel.setUuid(event.getMetadata().getUuid());
        responseModel.setName(payload.getExperimentName());
        responseModel.setDescription(payload.getDescription());
        responseModel.setExperimentType(payload.getExperimentType());
        responseModel.setProjectUuid(payload.getProjectUuid());

        return responseModel;
    }

    @NonNull
    public ExperimentResponseModel transformExperimentResponseModelFromEntity(@NonNull Experiment experiment, @NonNull ExperimentOpsHeaders headers) {

        log.info(headers, "transforming the Experiment entity to Experiment Response Model");

        ExperimentResponseModel responseModel = new ExperimentResponseModel();
        responseModel.setUuid(experiment.getUuid());
        responseModel.setName(experiment.getName());
        responseModel.setDescription(experiment.getDescription());
        responseModel.setExperimentType(experiment.getExperimentType());
        responseModel.setProjectUuid(experiment.getProjectUuid());

        return responseModel;
    }

    @NonNull
    public ExperimentListItemModel transformExperimentListItemModel(@NonNull Experiment experiment, int configCount, int runCount, @NonNull ExperimentOpsHeaders headers) {

        log.info(headers, "transforming the Experiment entity to Experiment List Item Model");

        ExperimentListItemModel listItem = new ExperimentListItemModel();
        listItem.setExperimentUuid(experiment.getUuid());
        listItem.setName(experiment.getName());
        listItem.setDescription(experiment.getDescription());
        listItem.setExperimentType(experiment.getExperimentType());
        listItem.setConfigCount(configCount);
        listItem.setRunCount(runCount);
        listItem.setCreatedAt(experiment.getCreationDate().toLocalDateTime().toString());

        return listItem;
    }

    @NonNull
    public Experiment transformExperimentEntity(@NonNull ExperimentMutationEvent event, @NonNull ExperimentOpsHeaders headers) {

        log.info(headers, "transforming the payload to Experiment Entity");

        ExperimentMutationEventPayload payload = event.getPayload();
        Experiment experiment = Experiment.builder()
                .name(payload.getExperimentName())
                .description(payload.getDescription())
                .experimentType(payload.getExperimentType())
                .projectUuid(payload.getProjectUuid())
                .workspaceUuid(event.getMetadata().getWorkspaceUuid())
                .status(StatusEnum.ACTIVE)
                .build();
        experiment.setUuid(event.getMetadata().getUuid());

        return experiment;
    }

}
