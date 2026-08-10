package com.experimentops.platformapi.transformer;

import com.experimentops.avroevent.type.EventType;
import com.experimentops.common.kafka.model.event.ExperimentOpsMetadataEvent;
import com.experimentops.common.kafka.utils.ExperimentOpsMetadataUtil;
import com.experimentops.experiment.run.event.*;
import com.experimentops.experiment.run.model.v1.*;
import com.experimentops.platformapi.dal.repository.ExperimentConfigWithTypeProjection;
import com.experimentops.platformapi.model.ExperimentRunConfigContext;
import com.experimentops.platformapi.model.ExperimentRunDetailArtifact;
import com.experimentops.platformapi.model.ExperimentRunDetailDataset;
import com.experimentops.platformapi.model.ExperimentRunDetailSummary;
import com.experimentops.platformapi.model.ExperimentRunExecutionConfig;
import com.experimentops.platformapi.model.ExperimentRunListItemProjection;
import com.experimentops.platformapi.model.ExperimentRunResolvedPlan;
import com.experimentops.platformapi.model.entity.*;
import com.experimentops.platformapi.model.type.DatasetFileFormatEnum;
import com.experimentops.platformapi.model.type.ExperimentStatusEnum;
import com.experimentops.run.artifact.model.v1.RunArtifactResponseModel;
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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.time.Duration;
import java.util.Objects;
import java.util.stream.Collectors;

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
                .progress(0)
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
                .experimentConfigName(projection.getName())
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
            ExperimentRunResolvedPlan.DatasetAttachment datasetAttachment,
            @NonNull ExperimentOpsHeaders headers) {

        log.info(headers, "transforming the payload to Run Dataset Entity");

        RunDataset runDataset = RunDataset.builder()
                .experimentRunUuid(experimentRun.getUuid())
                .datasetVersionUuid(datasetVersion.getUuid())
                .workspaceUuid(headers.getWorkspaceUuid())
                .usage(RUN_DATASET_USAGE_INPUT)
                .stepCount(datasetAttachment.stepCount())
                .portName(datasetAttachment.portName())
                .build();
        runDataset.setUuid(ExperimentOpsUtils.uuid());

        return runDataset;
    }

    @NonNull
    public RunArtifact transformRunArtifactEntity(
            @NonNull String experimentRunUuid,
            @NonNull ExperimentRunCompletedArtifact artifact,
            @NonNull RunArtifactStatusEnum status,
            @NonNull DownStreamPolicyEnum downStreamPolicy,
            @NonNull ExperimentOpsHeaders headers) {

        log.info(headers, "transforming the payload to Run Artifact Entity");

        RunArtifact runArtifact = RunArtifact.builder()
                .experimentRunUuid(experimentRunUuid)
                .workspaceUuid(headers.getWorkspaceUuid())
                .artifactType(artifact.getType())
                .storageUri(artifact.getUri())
                .experimentType(artifact.getExperimentType())
                .format(artifact.getFormat())
                .size(artifact.getSize())
                .stepCount(artifact.getStepCount())
                .portName(artifact.getPortName())
                .status(status)
                .downStreamPolicy(downStreamPolicy)
                .build();
        runArtifact.setUuid(ExperimentOpsUtils.uuid());

        return runArtifact;
    }

    @NonNull
    public RunArtifactResponseModel transformRunArtifactListResponseModel(
            @NonNull RunArtifact runArtifact,
            @NonNull ExperimentOpsHeaders headers) {

        log.info(headers, "transforming the Run Artifact entity to Run Artifact response model");

        return new RunArtifactResponseModel()
                .uuid(runArtifact.getUuid())
                .artifactType(runArtifact.getArtifactType())
                .format(runArtifact.getFormat())
                .size(Math.toIntExact(runArtifact.getSize()))
                .stepCount(runArtifact.getStepCount())
                .portName(runArtifact.getPortName())
                .status(runArtifact.getStatus().name())
                .experimentType(runArtifact.getExperimentType())
                .downstreamPolicy(runArtifact.getDownStreamPolicy().name());
    }

    @NonNull
    public ExperimentRunDetailResponseModel transformExperimentRunDetailResponseModel(
            @NonNull ExperimentRunDetailSummary summary,
            @NonNull List<ExperimentRunDetailDataset> datasets,
            @NonNull List<ExperimentRunDetailArtifact> primaryArtifacts,
            @NonNull Map<String, String> experimentTypesByConfigUuid,
            @NonNull Map<String, DatasetVersion> datasetVersionsByUuid,
            @NonNull List<RunArtifact> runArtifacts,
            @NonNull ExperimentOpsHeaders headers) {

        log.info(headers, "transforming the Experiment Run entity to Experiment Run detail response");

        ExperimentRunDetailResponseModel responseModel = new ExperimentRunDetailResponseModel();
        responseModel.setUuid(summary.uuid());
        responseModel.setName(summary.name());
        responseModel.setStatus(summary.experimentStatus() == null ? null : summary.experimentStatus().name());
        responseModel.setMessage(summary.message());
        responseModel.setExperimentUUID(summary.experimentUuid());
        responseModel.setExperimentName(summary.experimentName());
        responseModel.setProjectUUID(summary.projectUuid());
        responseModel.setProjectName(summary.projectName());
        responseModel.setCreationDate(toOffsetDateTime(summary.creationDate()));
        responseModel.setLastUpdated(toOffsetDateTime(summary.lastUpdated()));
        responseModel.setCreatedBy(summary.createdBy());
        responseModel.setArtifactCount(summary.artifactCount());
        responseModel.setNumSteps(summary.executionMode().size());
        responseModel.setDatasets(transformExperimentRunDatasetModels(datasets));
        responseModel.setRunArtifacts(transformExperimentRunArtifactModels(primaryArtifacts));
        responseModel.setCompletedSteps(summary.completedSteps());
        responseModel.setExecutionMode(transformExperimentRunDetailExecutionModeModels(
                summary.executionMode(),
                experimentTypesByConfigUuid,
                datasetVersionsByUuid,
                runArtifacts
        ));
        return responseModel;
    }

    @NonNull
    private List<ExperimentRunDatasetModel> transformExperimentRunDatasetModels(
            @NonNull List<ExperimentRunDetailDataset> datasets) {

        return datasets
                .stream()
                .map(dataset -> new ExperimentRunDatasetModel()
                        .datasetVersionUuid(dataset.datasetVersionUuid())
                        .name(dataset.name()))
                .toList();
    }

    @NonNull
    private List<ExperimentRunArtifactModel> transformExperimentRunArtifactModels(
            @NonNull List<ExperimentRunDetailArtifact> runArtifacts) {

        return runArtifacts
                .stream()
                .map(runArtifact -> new ExperimentRunArtifactModel()
                        .uuid(runArtifact.uuid())
                        .portName(runArtifact.portName()))
                .toList();
    }

    @NonNull
    private List<ExperimentRunDetailExecutionModeModel> transformExperimentRunDetailExecutionModeModels(
            List<ExecutionMode> executionMode,
            @NonNull Map<String, String> experimentTypesByConfigUuid,
            @NonNull Map<String, DatasetVersion> datasetVersionsByUuid,
            @NonNull List<RunArtifact> runArtifacts) {

        if (executionMode == null) {
            return List.of();
        }
        Map<Integer, List<RunArtifact>> runArtifactsByStepCount = runArtifacts
                .stream()
                .filter(runArtifact -> runArtifact.getStepCount() != null)
                .collect(Collectors.groupingBy(RunArtifact::getStepCount));
        return executionMode
                .stream()
                .map(executionModeItem -> new ExperimentRunDetailExecutionModeModel()
                        .stepCount(executionModeItem.getStepCount())
                        .experimentConfigUuid(executionModeItem.getExperimentConfigUuid())
                        .experimentType(experimentTypesByConfigUuid.get(executionModeItem.getExperimentConfigUuid()))
                        .inputs(transformResolvedInputModels(
                                executionModeItem.getInputs(),
                                datasetVersionsByUuid,
                                runArtifacts
                        ))
                        .outputs(transformStepOutputModels(
                                runArtifactsByStepCount.getOrDefault(
                                        executionModeItem.getStepCount(),
                                        Collections.emptyList()
                                )
                        )))
                .toList();
    }

    @NonNull
    private List<ExperimentRunResolvedInputModel> transformResolvedInputModels(
            List<ExecutionModeInput> inputs,
            @NonNull Map<String, DatasetVersion> datasetVersionsByUuid,
            @NonNull List<RunArtifact> runArtifacts) {

        if (inputs == null) {
            return List.of();
        }
        return inputs
                .stream()
                .map(input -> transformResolvedInputModel(input, datasetVersionsByUuid, runArtifacts))
                .toList();
    }

    @NonNull
    private ExperimentRunResolvedInputModel transformResolvedInputModel(
            @NonNull ExecutionModeInput input,
            @NonNull Map<String, DatasetVersion> datasetVersionsByUuid,
            @NonNull List<RunArtifact> runArtifacts) {

        ExperimentRunResolvedInputModel model = new ExperimentRunResolvedInputModel()
                .portName(input.getPortName());
        if (input.getInputType() != null) {
            model.setInputType(ExperimentRunResolvedInputModel.InputTypeEnum.fromValue(input.getInputType()));
        }
        if ("DATASET".equals(input.getInputType())) {
            DatasetVersion datasetVersion = datasetVersionsByUuid.get(input.getFile());
            model.setFileUuid(input.getFile());
            if (datasetVersion != null) {
                model.setName(datasetVersion.getName());
                model.setFormat(datasetVersion.getFormat());
            }
            return model;
        }

        RunArtifact sourceArtifact = findSourceArtifact(input, runArtifacts);
        if (sourceArtifact != null) {
            model.setFileUuid(sourceArtifact.getUuid());
            model.setName(artifactDisplayName(sourceArtifact));
            model.setFormat(sourceArtifact.getFormat());
        } else {
            model.setFileUuid(input.getFile());
            model.setName(input.getFile());
        }
        return model;
    }

    private RunArtifact findSourceArtifact(
            @NonNull ExecutionModeInput input,
            @NonNull List<RunArtifact> runArtifacts) {

        return runArtifacts
                .stream()
                .filter(runArtifact -> Objects.equals(runArtifact.getStepCount(), input.getSourceStepCount()))
                .filter(runArtifact -> Objects.equals(runArtifact.getPortName(), input.getFile()))
                .findFirst()
                .orElse(null);
    }

    @NonNull
    private List<ExperimentRunStepOutputModel> transformStepOutputModels(@NonNull List<RunArtifact> runArtifacts) {
        return runArtifacts
                .stream()
                .map(runArtifact -> new ExperimentRunStepOutputModel()
                        .artifactUuid(runArtifact.getUuid())
                        .portName(runArtifact.getPortName())
                        .name(artifactDisplayName(runArtifact))
                        .format(runArtifact.getFormat())
                        .size(runArtifact.getSize())
                        .status(runArtifact.getStatus() == null ? null : runArtifact.getStatus().name())
                        .downstreamPolicy(runArtifact.getDownStreamPolicy() == null ? null : runArtifact.getDownStreamPolicy().name()))
                .toList();
    }

    @NonNull
    private String artifactDisplayName(@NonNull RunArtifact runArtifact) {
        if (StringUtils.isBlank(runArtifact.getFormat())) {
            return runArtifact.getPortName();
        }
        return runArtifact.getPortName() + "." + runArtifact.getFormat().toLowerCase();
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
    public List<ExperimentRunExecutionModeModel> transformExecutionModeModel(List<ExecutionMode> executionMode) {
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

    private OffsetDateTime toOffsetDateTime(Timestamp timestamp) {
        return timestamp == null ? null : OffsetDateTime.ofInstant(timestamp.toInstant(), ZoneOffset.UTC);
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

    public ExperimentRunLogListResponseModel transformExperimentRunLogListResponseModel(@NonNull Page<ExperimentRunLog> experimentRunLogPage) {
        ExperimentRunLogListResponseModel responseModel = new ExperimentRunLogListResponseModel();
        responseModel.setData(experimentRunLogPage
                .getContent()
                .stream()
                .map(this::transformExperimentRunLogResponse)
                .toList());
        responseModel.setTotalElements(experimentRunLogPage.getTotalElements());
        return responseModel;
    }

    private ExperimentRunLogResponse transformExperimentRunLogResponse(@NonNull ExperimentRunLog experimentRunLog) {
        return new ExperimentRunLogResponse()
                .sequence(experimentRunLog.getSequence())
                .timestamp(toOffsetDateTime(experimentRunLog.getTimestamp()))
                .level(experimentRunLog.getLevel())
                .experimentType(experimentRunLog.getExperimentType())
                .message(experimentRunLog.getMessage());
    }

    public ExperimentRunLog transformExperimentRunLog(@NonNull ExperimentRunUserLogEvent log, @NonNull String experimentRunUuid, long sequence, @NonNull ExperimentOpsHeaders headers) {
        ExperimentRunLog experimentRunLog = ExperimentRunLog.builder()
                .experimentRunUuid(experimentRunUuid)
                .sequence(sequence)
                .level(log.getLevel())
                .timestamp(Timestamp.from(log.getTimestamp()))
                .experimentType(log.getExperimentType())
                .message(log.getMessage())
                .build();
        experimentRunLog.setCreatedBy(headers.getUserUuid());
        experimentRunLog.setUpdatedBy(headers.getUserUuid());
        return experimentRunLog;
    }
}
