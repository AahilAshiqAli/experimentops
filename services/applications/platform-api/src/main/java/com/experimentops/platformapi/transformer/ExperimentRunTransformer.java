package com.experimentops.platformapi.transformer;

import com.experimentops.avroevent.type.EventType;
import com.experimentops.common.kafka.model.event.ExperimentOpsMetadataEvent;
import com.experimentops.common.kafka.utils.ExperimentOpsMetadataUtil;
import com.experimentops.experiment.run.event.ExperimentRunEvent;
import com.experimentops.experiment.run.event.ExperimentRunEventPayload;
import com.experimentops.experiment.run.model.v1.*;
import com.experimentops.platformapi.dal.repository.ExperimentConfigWithTypeProjection;
import com.experimentops.platformapi.model.ExperimentRunConfigContext;
import com.experimentops.platformapi.model.ExperimentRunExecutionConfig;
import com.experimentops.platformapi.model.ExperimentRunListItemProjection;
import com.experimentops.platformapi.model.entity.DatasetVersion;
import com.experimentops.platformapi.model.entity.ExecutionMode;
import com.experimentops.platformapi.model.entity.Experiment;
import com.experimentops.platformapi.model.entity.ExperimentRun;
import com.experimentops.platformapi.model.entity.ExperimentTypeFormatMapping;
import com.experimentops.platformapi.model.entity.RunDataset;
import com.experimentops.platformapi.model.type.ExperimentStatusEnum;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.ExperimentOpsUtils;
import com.experimentops.utils.JSONUtil;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.time.Duration;

@Component
public class ExperimentRunTransformer {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(ExperimentRunTransformer.class);
    private static final String RUN_DATASET_USAGE_INPUT = "INPUT";

    @NonNull
    public ExperimentRunEvent transformExperimentRunEvent(
            @NonNull Experiment experiment,
            @NonNull String datasetUri,
            @NonNull List<ExperimentRunExecutionConfig> executionConfigs,
            @NonNull String experimentRunUuid,
            @NonNull ExperimentOpsHeaders headers
            ) {

        log.info(headers, "transforming the payload to Experiment Run Event");

        ExperimentRunEventPayload payload = ExperimentRunEventPayload.newBuilder()
                .setProjectUuid(experiment.getProjectUuid())
                .setWorkspaceUuid(headers.getWorkspaceUuid())
                .setConfigJson(JSONUtil.toNonTypedJsonFromObject(executionConfigs))
                .setDatasetUri(datasetUri)
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
            String name,
            @NonNull List<ExecutionMode> executionMode,
            @NonNull ExperimentOpsHeaders headers) {

        log.info(headers, "transforming the payload to Experiment Run Entity");

        ExperimentRun experimentRun = ExperimentRun.builder()
                .name(name)
                .experimentUuid(experimentUuid)
                .executionMode(executionMode)
                .workspaceUuid(headers.getWorkspaceUuid())
                .experimentStatus(ExperimentStatusEnum.PENDING)
                .runNumber(1)
                .build();
        experimentRun.setUuid(ExperimentOpsUtils.uuid());
        experimentRun.setCreatedBy(headers.getUserUuid());
        experimentRun.setUpdatedBy(headers.getUserUuid());
        return experimentRun;
    }

    @NonNull
    public List<ExecutionMode> transformExecutionMode(@NonNull List<ExperimentRunExecutionModeModel> executionMode) {
        return executionMode
                .stream()
                .map(executionModeItem -> ExecutionMode
                        .builder()
                        .stepCount(executionModeItem.getStepCount())
                        .experimentConfigUuid(executionModeItem.getExperimentConfigUuid())
                        .build())
                .toList();
    }

    @NonNull
    public List<ExperimentRunExecutionConfig> transformExperimentRunExecutionConfigs(
            @NonNull List<ExperimentRunExecutionModeModel> executionMode,
            @NonNull Map<String, ExperimentRunConfigContext> experimentConfigsByUuid) {

        return executionMode
                .stream()
                .map(executionModeItem -> {
                    ExperimentRunConfigContext experimentConfig = experimentConfigsByUuid.get(executionModeItem.getExperimentConfigUuid());
                    return ExperimentRunExecutionConfig
                            .builder()
                            .stepCount(executionModeItem.getStepCount())
                            .experimentType(experimentConfig.getExperimentType())
                            .experimentConfigJson(experimentConfig.getExperimentConfigJson())
                            .timeWeight(experimentConfig.getTimeWeight())
                            .build();
                })
                .toList();
    }

    @NonNull
    public ExperimentRunConfigContext transformExperimentRunConfigContext(@NonNull ExperimentConfigWithTypeProjection projection) {
        JsonNode experimentConfigJson = StringUtils.isBlank(projection.getConfig())
                ? null
                : JSONUtil.toObjectFromTypedJson(projection.getConfig(), JsonNode.class);
        List<ExperimentTypeFormatMapping> formatMappings = StringUtils.isBlank(projection.getFormatMappings())
                ? List.of()
                : JSONUtil.toListFromTypedJson(projection.getFormatMappings(), ExperimentTypeFormatMapping.class);
        return ExperimentRunConfigContext
                .builder()
                .experimentConfigUuid(projection.getExperimentConfigUuid())
                .experimentType(projection.getExperimentType())
                .experimentConfigJson(experimentConfigJson)
                .formatMappings(formatMappings)
                .timeWeight(projection.getTimeWeight())
                .build();
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

    @NonNull
    public ExperimentRunResponse transformExperimentRunResponseFromProjection(
            @NonNull ExperimentRunListItemProjection experimentRun,
            @NonNull ExperimentOpsHeaders headers) {

        log.info(headers, "transforming the Experiment Run projection to Experiment Run list item");

        ExperimentRunResponse response = new ExperimentRunResponse();
        response.setUuid(experimentRun.getUuid());
        response.setName(experimentRun.getName());
        response.setProgress(experimentRun.getProgress());
        response.setDatasetCount(experimentRun.getDatasetCount().intValue());
        response.setStatus(experimentRun.getExperimentStatus().name());
        response.setDuration(formatDuration(experimentRun.getCreationDate(), experimentRun.getLastUpdated()));
        response.setExecutionMode(transformExecutionModeModel(experimentRun.getExecutionMode()));

        return response;
    }

    @NonNull
    public ExperimentRunStatusResponseModel transformExperimentRunStatusResponseModel(@NonNull ExperimentRun experimentRun) {
        ExperimentRunStatusResponseModel responseModel = new ExperimentRunStatusResponseModel();
        responseModel.setExperimentRunUuid(experimentRun.getUuid());
        responseModel.setStatus(experimentRun.getExperimentStatus().name());
        responseModel.setProgress(experimentRun.getProgress());
        responseModel.setDuration(formatDuration(experimentRun.getCreationDate(), experimentRun.getLastUpdated()));
        return responseModel;
    }

    @NonNull
    private List<ExperimentRunExecutionModeModel> transformExecutionModeModel(List<ExecutionMode> executionMode) {
        if (executionMode == null) {
            return List.of();
        }
        return executionMode
                .stream()
                .map(executionModeItem -> new ExperimentRunExecutionModeModel()
                        .stepCount(executionModeItem.getStepCount())
                        .experimentConfigUuid(executionModeItem.getExperimentConfigUuid()))
                .toList();
    }


    @NonNull
    private String formatDuration(Timestamp creationDate, Timestamp lastUpdated) {

        Duration duration = Duration.between(creationDate.toInstant(), lastUpdated.toInstant());
        long seconds = Math.max(0, duration.getSeconds());
        return String.format("%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
    }

    public ExperimentRunListResponseModel transformExperimentRunListResponseModel(@NonNull Page<ExperimentRunListItemProjection> experimentRunPage, @NonNull ExperimentOpsHeaders headers) {

        List<ExperimentRunResponse> experimentRunResponses = experimentRunPage
                .getContent()
                .stream()
                .map(experimentRun -> transformExperimentRunResponseFromProjection(experimentRun, headers))
                .toList();

        ExperimentRunListResponseModel experimentRunListResponseModel = new ExperimentRunListResponseModel();
        experimentRunListResponseModel.setData(experimentRunResponses);
        experimentRunListResponseModel.setTotalElements(experimentRunPage.getTotalElements());

        return experimentRunListResponseModel;
    }
}
