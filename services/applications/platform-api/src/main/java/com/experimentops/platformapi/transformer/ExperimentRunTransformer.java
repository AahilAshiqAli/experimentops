package com.experimentops.platformapi.transformer;

import com.experimentops.avroevent.type.EventType;
import com.experimentops.common.kafka.model.event.ExperimentOpsMetadataEvent;
import com.experimentops.common.kafka.utils.ExperimentOpsMetadataUtil;
import com.experimentops.experiment.run.event.ExperimentRunEvent;
import com.experimentops.experiment.run.event.ExperimentRunEventPayload;
import com.experimentops.experiment.run.event.ExperimentRunExecutionPlan;
import com.experimentops.experiment.run.event.ExperimentRunExecutionPlanInput;
import com.experimentops.experiment.run.event.ExperimentRunExecutionPlanOutput;
import com.experimentops.experiment.run.event.ExperimentRunExecutionPlanStep;
import com.experimentops.experiment.run.event.ExperimentRunPlanDataFormat;
import com.experimentops.experiment.run.event.ExperimentRunPlanDataKind;
import com.experimentops.experiment.run.event.ExperimentRunPlanDownStreamPolicy;
import com.experimentops.experiment.run.event.ExperimentRunPlanFormatStrategy;
import com.experimentops.experiment.run.event.ExperimentRunPlanInputType;
import com.experimentops.experiment.run.model.v1.*;
import com.experimentops.platformapi.dal.repository.ExperimentConfigWithTypeProjection;
import com.experimentops.platformapi.model.ExperimentRunConfigContext;
import com.experimentops.platformapi.model.ExperimentRunExecutionConfig;
import com.experimentops.platformapi.model.ExperimentRunListItemProjection;
import com.experimentops.platformapi.model.ExperimentRunResolvedPlan;
import com.experimentops.platformapi.model.entity.DatasetVersion;
import com.experimentops.platformapi.model.entity.DownStreamPolicyEnum;
import com.experimentops.platformapi.model.entity.ExecutionMode;
import com.experimentops.platformapi.model.entity.ExecutionModeInput;
import com.experimentops.platformapi.model.entity.Experiment;
import com.experimentops.platformapi.model.entity.ExperimentRun;
import com.experimentops.platformapi.model.entity.ExperimentTypeManifest;
import com.experimentops.platformapi.model.entity.FormatStrategyTypeEnum;
import com.experimentops.platformapi.model.entity.RunDataset;
import com.experimentops.platformapi.model.type.DatasetFileFormatEnum;
import com.experimentops.platformapi.model.type.ExperimentStatusEnum;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.ExperimentOpsUtils;
import com.experimentops.utils.JSONUtil;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.openapitools.jackson.nullable.JsonNullable;
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
            @NonNull ExperimentRunResolvedPlan resolvedPlan,
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
                .setExecutionPlan(transformExecutionPlanEvent(resolvedPlan))
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
                        .inputs(transformExecutionModeInputs(executionModeItem.getInputs()))
                        .build())
                .toList();
    }

    @NonNull
    private List<ExecutionModeInput> transformExecutionModeInputs(List<ExperimentRunInputModel> inputs) {
        if (inputs == null) {
            return List.of();
        }
        return inputs.stream()
                .map(input -> ExecutionModeInput.builder()
                        .portName(input.getPortName())
                        .inputType(input.getInputType() == null ? null : input.getInputType().getValue())
                        .file(input.getFile())
                        .sourceStepCount(getNullableInteger(input.getSourceStepCount()))
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
        List<ExperimentTypeManifest> formatMappings = StringUtils.isBlank(projection.getFormatMappings())
                ? List.of()
                : JSONUtil.toListFromTypedJson(projection.getFormatMappings(), ExperimentTypeManifest.class);
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
            @NonNull List<String> runDatasetUuids,
            @NonNull ExperimentOpsHeaders headers) {

        log.info(headers, "transforming the Experiment Run entity to Experiment Run Response Model");

        ExperimentRunResponseModel responseModel = new ExperimentRunResponseModel();
        responseModel.setExperimentRunUuid(experimentRun.getUuid());
        responseModel.setRunDatasetUuids(runDatasetUuids);
        responseModel.setRunDatasetUuid(runDatasetUuids.isEmpty() ? null : runDatasetUuids.getFirst());
        responseModel.setStatus(experimentRun.getExperimentStatus().name());

        return responseModel;
    }

    @NonNull
    private ExperimentRunExecutionPlan transformExecutionPlanEvent(@NonNull ExperimentRunResolvedPlan resolvedPlan) {
        return ExperimentRunExecutionPlan.newBuilder()
                .setSchemaVersion(1)
                .setSteps(resolvedPlan.getSteps().stream()
                        .map(this::transformExecutionPlanStepEvent)
                        .toList())
                .build();
    }

    @NonNull
    private ExperimentRunExecutionPlanStep transformExecutionPlanStepEvent(ExperimentRunResolvedPlan.Step step) {
        return ExperimentRunExecutionPlanStep.newBuilder()
                .setStepCount(step.getStepCount())
                .setExperimentConfigUuid(step.getExperimentConfigUuid())
                .setExperimentType(step.getExperimentType())
                .setExperimentConfigJson(step.getExperimentConfigJson() == null ? null : step.getExperimentConfigJson().toString())
                .setTimeWeight(step.getTimeWeight())
                .setInputs(step.getInputs().stream()
                        .map(this::transformExecutionPlanInputEvent)
                        .toList())
                .setOutputs(step.getOutputs().stream()
                        .map(this::transformExecutionPlanOutputEvent)
                        .toList())
                .build();
    }

    @NonNull
    private ExperimentRunExecutionPlanInput transformExecutionPlanInputEvent(ExperimentRunResolvedPlan.Input input) {
        return ExperimentRunExecutionPlanInput.newBuilder()
                .setPortName(input.getPortName())
                .setInputType(toPlanInputType(input.getInputType()))
                .setDataKind(toPlanDataKind(input.getDataKind()))
                .setFormat(toPlanDataFormat(input.getFormat()))
                .setDatasetVersionUuid(input.getDatasetVersionUuid())
                .setDatasetUri(input.getDatasetUri())
                .setSourceStepCount(input.getSourceStepCount())
                .setArtifactName(input.getArtifactName())
                .build();
    }

    @NonNull
    private ExperimentRunExecutionPlanOutput transformExecutionPlanOutputEvent(ExperimentRunResolvedPlan.Output output) {
        return ExperimentRunExecutionPlanOutput.newBuilder()
                .setName(output.getName())
                .setDataKind(toPlanDataKind(output.getDataKind()))
                .setFormatStrategy(toPlanFormatStrategy(output.getFormatStrategy()))
                .setFormat(toPlanDataFormat(output.getFormat()))
                .setSourceInputPort(output.getSourceInputPort())
                .setRequiredForRun(output.getRequiredForRun())
                .setDownStreamPolicy(toPlanDownStreamPolicy(output.getDownStreamPolicy()))
                .build();
    }

    private ExperimentRunPlanInputType toPlanInputType(String inputType) {
        return inputType == null ? null : ExperimentRunPlanInputType.valueOf(inputType);
    }

    private ExperimentRunPlanDataKind toPlanDataKind(String dataKind) {
        return dataKind == null ? null : ExperimentRunPlanDataKind.valueOf(dataKind);
    }

    private ExperimentRunPlanDataFormat toPlanDataFormat(DatasetFileFormatEnum format) {
        return format == null ? null : ExperimentRunPlanDataFormat.valueOf(format.name());
    }

    private ExperimentRunPlanFormatStrategy toPlanFormatStrategy(FormatStrategyTypeEnum formatStrategy) {
        return formatStrategy == null ? null : ExperimentRunPlanFormatStrategy.valueOf(formatStrategy.name());
    }

    private ExperimentRunPlanDownStreamPolicy toPlanDownStreamPolicy(DownStreamPolicyEnum downStreamPolicy) {
        return downStreamPolicy == null ? null : ExperimentRunPlanDownStreamPolicy.valueOf(downStreamPolicy.name());
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
                        .experimentConfigUuid(executionModeItem.getExperimentConfigUuid())
                        .inputs(transformExecutionModeInputModels(executionModeItem.getInputs())))
                .toList();
    }

    @NonNull
    private List<ExperimentRunInputModel> transformExecutionModeInputModels(List<ExecutionModeInput> inputs) {
        if (inputs == null) {
            return List.of();
        }
        return inputs.stream()
                .map(input -> {
                    ExperimentRunInputModel model = new ExperimentRunInputModel()
                            .portName(input.getPortName())
                            .file(input.getFile());
                    if (input.getInputType() != null) {
                        model.setInputType(ExperimentRunInputModel.InputTypeEnum.fromValue(input.getInputType()));
                    }
                    if (input.getSourceStepCount() != null) {
                        model.setSourceStepCount(JsonNullable.of(input.getSourceStepCount()));
                    }
                    return model;
                })
                .toList();
    }

    private Integer getNullableInteger(JsonNullable<Integer> value) {
        if (value == null || !value.isPresent()) {
            return null;
        }
        return value.get();
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
