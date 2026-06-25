package com.experimentops.platformapi.transformer;

import com.experimentops.avroevent.type.EventType;
import com.experimentops.common.kafka.model.event.ExperimentOpsMetadataEvent;
import com.experimentops.common.kafka.utils.ExperimentOpsMetadataUtil;
import com.experimentops.experiment.run.event.ExperimentRunEvent;
import com.experimentops.experiment.run.event.ExperimentRunEventPayload;
import com.experimentops.experiment.run.model.v1.ExperimentRunRequestModel;
import com.experimentops.experiment.run.model.v1.ExperimentRunResponseModel;
import com.experimentops.platformapi.model.entity.DatasetVersion;
import com.experimentops.platformapi.model.entity.Experiment;
import com.experimentops.platformapi.model.entity.ExperimentRun;
import com.experimentops.platformapi.model.entity.RunDataset;
import com.experimentops.platformapi.model.type.ExperimentStatusEnum;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.ExperimentOpsUtils;
import com.experimentops.utils.JSONUtil;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

@Component
public class ExperimentRunTransformer {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(ExperimentRunTransformer.class);
    private static final String RUN_DATASET_USAGE_INPUT = "INPUT";

    @NonNull
    public ExperimentRunEvent transformExperimentRunEvent(
            @NonNull Experiment experiment,
            @NonNull String datasetUri,
            @NonNull ExperimentRunRequestModel requestModel,
            @NonNull String experimentRunUuid,
            @NonNull ExperimentOpsHeaders headers
            ) {

        log.info(headers, "transforming the payload to Experiment Run Event");

        ExperimentRunEventPayload payload = ExperimentRunEventPayload.newBuilder()
                .setProjectUuid(experiment.getProjectUuid())
                .setWorkspaceUuid(headers.getWorkspaceUuid())
                .setConfigJson(JSONUtil.toNonTypedJsonFromObject(requestModel.getConfigJson()))
                .setDatasetUri(datasetUri)
                .setExperimentType(experiment.getExperimentType())
                .setExperimentUuid(experiment.getUuid())
                .build();

        ExperimentOpsMetadataEvent metadata = ExperimentOpsMetadataUtil.metadataEvent(
                headers,
                experimentRunUuid,
                EventType.EXPERIMENT_RUN_REQUESTED.name(),
                this.getClass().getSimpleName()
        );

        return ExperimentRunEvent.newBuilder()
                .setMetadata(metadata)
                .setPayload(payload)
                .build();
    }

    @NonNull
    public ExperimentRun transformExperimentRunEntity(
            @NonNull String experimentUuid,
            @NonNull ExperimentOpsHeaders headers) {

        log.info(headers, "transforming the payload to Experiment Run Entity");

        ExperimentRun experimentRun = ExperimentRun.builder()
                .experimentUuid(experimentUuid)
                .experimentConfigUuid(ExperimentOpsUtils.uuid())
                .workspaceUuid(headers.getWorkspaceUuid())
                .experimentStatus(ExperimentStatusEnum.PENDING)
                .runNumber(1)
                .build();
        experimentRun.setUuid(ExperimentOpsUtils.uuid());

        return experimentRun;
    }

    @NonNull
    public RunDataset transformRunDatasetEntity(
            @NonNull ExperimentRun experimentRun,
            @NonNull DatasetVersion datasetVersion,
            @NonNull ExperimentOpsHeaders headers) {

        log.info(headers, "transforming the payload to Run Dataset Entity");

        RunDataset runDataset = RunDataset.builder()
                .experimentRunUuid(experimentRun.getUuid())
                .datasetVersionUuid(datasetVersion.getUuid())
                .workspaceUuid(headers.getWorkspaceUuid())
                .usage(RUN_DATASET_USAGE_INPUT)
                .build();
        runDataset.setUuid(ExperimentOpsUtils.uuid());

        return runDataset;
    }

    @NonNull
    public ExperimentRunResponseModel transformExperimentRunResponseModelFromEntity(
            @NonNull ExperimentRun experimentRun,
            @NonNull String runDatasetUuid,
            @NonNull ExperimentOpsHeaders headers) {

        log.info(headers, "transforming the Experiment Run entity to Experiment Run Response Model");

        ExperimentRunResponseModel responseModel = new ExperimentRunResponseModel();
        responseModel.setExperimentRunUuid(experimentRun.getUuid());
        responseModel.setRunDatasetUuid(runDatasetUuid);
        responseModel.setStatus(experimentRun.getExperimentStatus().name());

        return responseModel;
    }
}
