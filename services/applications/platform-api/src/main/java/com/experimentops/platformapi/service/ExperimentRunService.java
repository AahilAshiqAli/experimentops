package com.experimentops.platformapi.service;

import com.experimentops.common.exceptions.constant.ErrorCode;
import com.experimentops.common.exceptions.runtime.EntityNotFoundException;
import com.experimentops.common.exceptions.runtime.ValidationException;
import com.experimentops.common.kafka.KafkaProducer;
import com.experimentops.experiment.run.event.*;
import com.experimentops.experiment.run.model.v1.*;
import com.experimentops.platformapi.dal.repository.*;
import com.experimentops.platformapi.model.ExperimentRunConfigContext;
import com.experimentops.platformapi.model.ExperimentRunDetailConfigType;
import com.experimentops.platformapi.model.ExperimentRunDetailSummary;
import com.experimentops.platformapi.model.ExperimentRunExecutionConfig;
import com.experimentops.platformapi.model.ExperimentRunListItemProjection;
import com.experimentops.platformapi.model.ExperimentRunResolvedPlan;
import com.experimentops.platformapi.model.ExperimentRunSearchCriteria;
import com.experimentops.platformapi.model.entity.*;
import com.experimentops.platformapi.model.type.ExperimentStatusEnum;
import com.experimentops.platformapi.model.type.StatusEnum;
import com.experimentops.platformapi.transformer.ExperimentRunTransformer;
import com.experimentops.platformapi.validator.ExperimentRunValidator;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExperimentRunService {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(ExperimentRunService.class);

    private final KafkaProducer kafkaProducer;
    private final ExperimentRunValidator experimentRunValidator;
    private final ExperimentRunTransformer experimentRunTransformer;
    private final ExperimentRepository experimentRepository;
    private final ExperimentConfigRepository experimentConfigRepository;
    private final ExperimentRunRepository experimentRunRepository;
    private final DatasetVersionRepository datasetVersionRepository;
    private final RunDatasetRepository runDatasetRepository;
    private final RunArtifactService runArtifactService;
    private final RunArtifactRepository runArtifactRepository;
    private final ExperimentRunLogRepository experimentRunLogRepository;

    @Value("${experiment.run.requested.topic}")
    private String experimentRunRequestTopic;

    @NonNull
    public ExperimentRunResponseModel publishExperimentRunRequest(@NonNull String experimentUuid, @NonNull ExperimentRunRequestModel experimentRunRequestModel, @NonNull ExperimentOpsHeaders headers){
        experimentRunValidator.validateExperimentRunRequestModel(experimentUuid, experimentRunRequestModel, true);
        Experiment experiment = getExperiment(experimentUuid, headers);
        List<ExperimentRunExecutionModeModel> requestedExecutionMode = experimentRunRequestModel.getExecutionMode()
                .stream()
                .sorted(Comparator.comparing(ExperimentRunExecutionModeModel::getStepCount))
                .toList();
        Map<String, DatasetVersion> datasetVersionsByUuid = getDatasetVersionsByUuid(
                experimentRunValidator.collectDatasetVersionUuids(requestedExecutionMode),
                headers
        );
        Map<String, ExperimentRunConfigContext> experimentConfigsByUuid = getExperimentConfigsByUuid(
                experimentUuid,
                requestedExecutionMode,
                headers
        );
        ExperimentRunResolvedPlan resolvedPlan = experimentRunValidator.validateAndResolveExecutionPlan(
                requestedExecutionMode,
                experimentConfigsByUuid,
                datasetVersionsByUuid
        );
        List<ExecutionMode> executionMode = experimentRunTransformer.transformExecutionMode(requestedExecutionMode);
        List<ExperimentRunExecutionConfig> executionConfigs = experimentRunTransformer.transformExperimentRunExecutionConfigs(
                requestedExecutionMode,
                experimentConfigsByUuid
        );

        ExperimentRun experimentRun = experimentRunTransformer.transformExperimentRunEntity(
                experimentUuid,
                experimentRunRequestModel.getName(),
                executionMode,
                headers
        );

        log.info(headers, "saving experiment run object");
        experimentRunRepository.save(experimentRun);

        List<RunDataset> runDatasets = resolvedPlan.datasetAttachments()
                .stream()
                .map(datasetAttachment -> experimentRunTransformer.transformRunDatasetEntity(
                        experimentRun,
                        datasetVersionsByUuid.get(datasetAttachment.datasetVersionUuid()),
                        datasetAttachment,
                        headers
                ))
                .toList();

        log.info(headers, "saving run dataset objects");
        runDatasetRepository.saveAll(runDatasets);

        ExperimentRunEvent experimentRunEvent = experimentRunTransformer.transformExperimentRunEvent(
                experiment,
                resolvedPlan.firstDatasetUri(),
                executionConfigs,
                resolvedPlan,
                experimentRun.getUuid(),
                headers
        );
        kafkaProducer.sendMessage(experimentRunRequestTopic, experimentRunEvent, experimentRunEvent.getMetadata());
        return experimentRunTransformer.transformExperimentRunResponseModelFromEntity(
                experimentRun,
                runDatasets.stream().map(RunDataset::getUuid).toList(),
                headers
        );
    }

    public void validateExperimentRunRequest(@NonNull String experimentUuid, @NonNull ExperimentRunRequestModel experimentRunRequestModel, @NonNull ExperimentOpsHeaders headers) {
        experimentRunValidator.validateExperimentRunRequestModel(experimentUuid, experimentRunRequestModel, false);
        getExperiment(experimentUuid, headers);
        List<ExperimentRunExecutionModeModel> requestedExecutionMode = experimentRunRequestModel.getExecutionMode()
                .stream()
                .sorted(Comparator.comparing(ExperimentRunExecutionModeModel::getStepCount))
                .toList();
        Map<String, DatasetVersion> datasetVersionsByUuid = getDatasetVersionsByUuid(
                experimentRunValidator.collectDatasetVersionUuids(requestedExecutionMode),
                headers
        );
        Map<String, ExperimentRunConfigContext> experimentConfigsByUuid = getExperimentConfigsByUuid(
                experimentUuid,
                requestedExecutionMode,
                headers
        );
        experimentRunValidator.validateAndResolveExecutionPlan(
                requestedExecutionMode,
                experimentConfigsByUuid,
                datasetVersionsByUuid
        );
    }

    private Map<String, DatasetVersion> getDatasetVersionsByUuid(
            @NonNull List<String> datasetVersionUuids,
            @NonNull ExperimentOpsHeaders headers) {

        if (datasetVersionUuids.isEmpty()) {
            return Map.of();
        }

        Map<String, DatasetVersion> datasetVersionsByUuid = datasetVersionRepository
                .findAllByUuidInAndWorkspaceUuidAndStatusAndEnabled(
                        datasetVersionUuids,
                        headers.getWorkspaceUuid(),
                        StatusEnum.ACTIVE,
                        true
                )
                .stream()
                .collect(Collectors.toMap(DatasetVersion::getUuid, Function.identity()));

        List<String> missingDatasetVersionUuids = datasetVersionUuids
                .stream()
                .filter(datasetVersionUuid -> !datasetVersionsByUuid.containsKey(datasetVersionUuid))
                .toList();

        if (!missingDatasetVersionUuids.isEmpty()) {
            throw new ValidationException(
                    ErrorCode.INVALID_INPUTS,
                    "Invalid datasetVersionUuid: " + String.join(", ", missingDatasetVersionUuids)
            );
        }

        return datasetVersionsByUuid;
    }

    private Map<String, ExperimentRunConfigContext> getExperimentConfigsByUuid(
            @NonNull String experimentUuid,
            @NonNull List<ExperimentRunExecutionModeModel> executionMode,
            @NonNull ExperimentOpsHeaders headers) {

        LinkedHashSet<String> experimentConfigUuids = executionMode
                .stream()
                .map(ExperimentRunExecutionModeModel::getExperimentConfigUuid)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<String, ExperimentRunConfigContext> experimentConfigsByUuid = experimentConfigRepository
                .findAllWithExperimentTypesByUuidIn(
                        experimentConfigUuids,
                        experimentUuid,
                        headers.getWorkspaceUuid(),
                        StatusEnum.ACTIVE.getCode()
                )
                .stream()
                .map(experimentRunTransformer::transformExperimentRunConfigContext)
                .collect(Collectors.toMap(ExperimentRunConfigContext::getExperimentConfigUuid, experimentConfig -> experimentConfig));

        List<String> missingExperimentConfigUuids = experimentConfigUuids
                .stream()
                .filter(experimentConfigUuid -> !experimentConfigsByUuid.containsKey(experimentConfigUuid))
                .toList();

        if (!missingExperimentConfigUuids.isEmpty()) {
            throw new ValidationException(
                    ErrorCode.INVALID_INPUTS,
                    "Invalid experimentConfigUuid: " + String.join(", ", missingExperimentConfigUuids)
            );
        }

        return experimentConfigsByUuid;
    }

    private Map<String, ExperimentRunConfigContext> getExperimentConfigsByUuidForCompletion(
            @NonNull ExperimentRun experimentRun,
            @NonNull List<ExperimentRunExecutionModeModel> executionMode,
            @NonNull ExperimentOpsHeaders headers) {

        LinkedHashSet<String> experimentConfigUuids = executionMode
                .stream()
                .map(ExperimentRunExecutionModeModel::getExperimentConfigUuid)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<String, ExperimentRunConfigContext> experimentConfigsByUuid = experimentConfigRepository
                .findAllWithExperimentTypesByUuidIn(
                        experimentConfigUuids,
                        experimentRun.getExperimentUuid(),
                        headers.getWorkspaceUuid(),
                        StatusEnum.ACTIVE.getCode()
                )
                .stream()
                .map(experimentRunTransformer::transformExperimentRunConfigContext)
                .collect(Collectors.toMap(ExperimentRunConfigContext::getExperimentConfigUuid, experimentConfig -> experimentConfig));

        experimentConfigUuids
                .stream()
                .filter(experimentConfigUuid -> !experimentConfigsByUuid.containsKey(experimentConfigUuid))
                .findFirst()
                .ifPresent(experimentConfigUuid -> {
                    Integer stepCount = executionMode
                            .stream()
                            .filter(executionModeItem -> experimentConfigUuid.equals(executionModeItem.getExperimentConfigUuid()))
                            .map(ExperimentRunExecutionModeModel::getStepCount)
                            .findFirst()
                            .orElse(null);
                    throw new ValidationException(
                            ErrorCode.INVALID_INPUTS,
                            "Experiment config " + experimentConfigUuid + " failed step number : " + stepCount
                                    + ". Experiment config not found or inactive"
                    );
                });

        return experimentConfigsByUuid;
    }

    @NonNull
    private Map<Integer, String> experimentConfigNamesByStep(
            @NonNull List<ExperimentRunExecutionModeModel> executionMode,
            @NonNull Map<String, ExperimentRunConfigContext> experimentConfigsByUuid) {

        return executionMode
                .stream()
                .collect(Collectors.toMap(
                        ExperimentRunExecutionModeModel::getStepCount,
                        executionModeItem -> {
                            ExperimentRunConfigContext experimentConfig = experimentConfigsByUuid.get(executionModeItem.getExperimentConfigUuid());
                            return experimentConfig.getExperimentConfigName();
                        }
                ));
    }

    public void processExperimentRunCompleted(@NonNull ExperimentRunCompletedEvent event, @NonNull ExperimentOpsHeaders headers) {
        ExperimentRun experimentRun = getExperimentRunEntity(event.getMetadata().getUuid(), headers);
        String logFileUrl = event.getPayload() == null ? null : event.getPayload().getLogFileUrl();
        try {
            List<ExperimentRunExecutionModeModel> executionMode = experimentRunTransformer.transformExecutionModeModel(
                    experimentRun.getExecutionMode()
            );
            Map<String, DatasetVersion> datasetVersionsByUuid = getDatasetVersionsByUuid(
                    experimentRunValidator.collectDatasetVersionUuids(executionMode),
                    headers
            );
            Map<String, ExperimentRunConfigContext> experimentConfigsByUuid = getExperimentConfigsByUuidForCompletion(
                    experimentRun,
                    executionMode,
                    headers
            );
            ExperimentRunResolvedPlan resolvedPlan = experimentRunValidator.validateAndResolveExecutionPlan(
                    executionMode,
                    experimentConfigsByUuid,
                    datasetVersionsByUuid
            );
            experimentRunValidator.validateCompletedArtifacts(
                    event,
                    resolvedPlan,
                    experimentConfigNamesByStep(executionMode, experimentConfigsByUuid)
            );
            runArtifactService.uploadArtifacts(event, experimentRun.getUuid(), resolvedPlan, headers);
            updateExperimentRunStatus(experimentRun, headers, ExperimentStatusEnum.SUCCEEDED, null, logFileUrl);
        } catch (ValidationException e) {
            updateExperimentRunStatus(experimentRun, headers, ExperimentStatusEnum.FAILED, e.getMessage(), logFileUrl);
        }

    }

    public void processExperimentRunFailure(@NonNull ExperimentRunFailureEvent event, @NonNull ExperimentOpsHeaders headers) {
        updateExperimentRunStatus(
                getExperimentRunEntity(event.getMetadata().getUuid(), headers),
                headers,
                ExperimentStatusEnum.FAILED,
                failureMessage(event),
                event.getPayload() == null ? null : event.getPayload().getLogFileUrl()
        );
    }

    private void updateExperimentRunStatus(
            @NonNull ExperimentRun experimentRun,
            @NonNull ExperimentOpsHeaders headers,
            @NonNull ExperimentStatusEnum status,
            @Nullable String message,
            @Nullable String logFileUrl) {

        log.info(headers, "updating experiment run status");
        experimentRun.setExperimentStatus(status);
        experimentRun.setMessage(message);
        if (!StringUtils.isBlank(logFileUrl)) {
            experimentRun.setLogs(logFileUrl);
        }
        if (status == ExperimentStatusEnum.SUCCEEDED) {
            experimentRun.setProgress(100);
        }
        experimentRunRepository.save(experimentRun);
    }

    @Nullable
    private String failureMessage(@NonNull ExperimentRunFailureEvent event) {
        ExperimentRunFailureEventPayload payload = event.getPayload();
        if (payload == null || payload.getErrors() == null || payload.getErrors().isEmpty()) {
            return null;
        }
        return payload.getErrors()
                .stream()
                .map(error -> error.getErrorType() + ": " + error.getErrorMessage())
                .collect(Collectors.joining("; "));
    }

    @NonNull
    private ExperimentRun getExperimentRunEntity(
            @NonNull String experimentRunUuid,
            @NonNull ExperimentOpsHeaders headers) {

        return experimentRunRepository
                .findByUuidAndWorkspaceUuidAndEnabled(experimentRunUuid, headers.getWorkspaceUuid(), true)
                .orElseThrow(() -> new EntityNotFoundException("experiment run uuid", experimentRunUuid));
    }

    @NonNull
    public ExperimentRunDetailResponseModel getExperimentRun(@NonNull String experimentRunUuid, @NonNull ExperimentOpsHeaders headers) {
        log.info(headers, "getting experiment run detail for uuid " + experimentRunUuid);
        ExperimentRunDetailSummary summary = experimentRunRepository
                .findExperimentRunDetailSummary(experimentRunUuid, headers.getWorkspaceUuid())
                .orElseThrow(() -> new EntityNotFoundException("experiment run uuid", experimentRunUuid));
        List<String> experimentConfigUuids = summary.executionMode()
                .stream()
                .map(ExecutionMode::getExperimentConfigUuid)
                .distinct()
                .toList();
        Map<String, String> experimentTypesByConfigUuid = experimentRunRepository
                .findExperimentRunDetailConfigTypes(
                        experimentConfigUuids,
                        summary.experimentUuid(),
                        headers.getWorkspaceUuid()
                )
                .stream()
                .collect(Collectors.toMap(
                        ExperimentRunDetailConfigType::experimentConfigUuid,
                        ExperimentRunDetailConfigType::experimentType
                ));
        List<RunArtifact> runArtifacts = runArtifactRepository
                .findByExperimentRunUuidAndWorkspaceUuidAndEnabledOrderByStepCountAscPortNameAsc(
                        experimentRunUuid,
                        headers.getWorkspaceUuid(),
                        true
                );
        List<String> datasetVersionUuids = collectDatasetVersionUuids(summary.executionMode());
        Map<String, DatasetVersion> datasetVersionsByUuid = getDatasetVersionsByUuid(datasetVersionUuids, headers);

        return experimentRunTransformer.transformExperimentRunDetailResponseModel(
                summary,
                experimentRunRepository.findExperimentRunDetailDatasets(experimentRunUuid, headers.getWorkspaceUuid()),
                experimentRunRepository.findExperimentRunDetailPrimaryArtifacts(experimentRunUuid, headers.getWorkspaceUuid()),
                experimentTypesByConfigUuid,
                datasetVersionsByUuid,
                runArtifacts,
                headers
        );
    }

    @NonNull
    private List<String> collectDatasetVersionUuids(@NonNull List<ExecutionMode> executionMode) {
        return executionMode
                .stream()
                .flatMap(executionModeItem -> executionModeItem.getInputs() == null
                        ? List.<ExecutionModeInput>of().stream()
                        : executionModeItem.getInputs().stream())
                .filter(input -> "DATASET".equals(input.getInputType()))
                .map(ExecutionModeInput::getFile)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();
    }

    public void processExperimentRunProgress(@NonNull ExperimentRunProgressEvent event, @NonNull ExperimentOpsHeaders headers) {
        ExperimentRunProgressEventPayload payload = event.getPayload();
        String experimentRunUuid = payload.getExperimentRunUuid();
        int progress = Integer.parseInt(payload.getProgress());
        ExperimentRun experimentRun = experimentRunRepository
                .findByUuidAndWorkspaceUuidAndEnabled(experimentRunUuid, headers.getWorkspaceUuid(), true)
                .orElseThrow(() -> new EntityNotFoundException("Experiment Run uuid", experimentRunUuid));
        experimentRun.setStepCompleted(experimentRun.getStepCompleted());
        experimentRun.setProgress(progress);
        experimentRunRepository.save(experimentRun);

        List<ExperimentRunUserLogEvent> logs = payload.getLogs();
        if (logs != null && !logs.isEmpty()) {
            List<ExperimentRunLog> experimentRunLogs = logs
                    .stream()
                    .map(experimentRunUserLogEvent -> experimentRunTransformer.transformExperimentRunLog(experimentRunUserLogEvent, experimentRunUuid, payload.getSequence(), headers))
                    .toList();
            experimentRunLogRepository.saveAll(experimentRunLogs);
        }
    }

    public ExperimentRunListResponseModel getExperimentRunList(@NonNull String experimentUuid, @Nullable String name, @Nullable String status, @Nullable Integer page, @Nullable Integer size, @NonNull ExperimentOpsHeaders headers) {
        log.info(headers, "getting experiment run list for experiment uuid " + experimentUuid);
        getExperiment(experimentUuid, headers);
        Pageable pageable = PaginationUtil.createPageRequest(page, size);
        List<ExperimentStatusEnum> experimentStatuses = experimentRunValidator.getExperimentStatuses(status);

        ExperimentRunSearchCriteria criteria = new ExperimentRunSearchCriteria(
                experimentUuid,
                name,
                experimentStatuses,
                headers.getWorkspaceUuid(),
                headers.getUserUuid()
        );
        Page<ExperimentRunListItemProjection> experimentRunPage = experimentRunRepository
                .searchExperimentRuns(criteria, pageable);

        return experimentRunTransformer.transformExperimentRunListResponseModel(experimentRunPage, headers);
    }

    public List<ExperimentRunStatusResponseModel> getExperimentRunStatus(
            @NonNull ExperimentRunStatusRequestModel requestModel,
            @NonNull ExperimentOpsHeaders headers) {

        log.info(headers, "getting experiment run status for requested uuids");
        List<String> experimentRunUuids = experimentRunValidator.validateExperimentRunStatusRequestModel(requestModel);

        Map<String, ExperimentRun> experimentRunsByUuid = experimentRunRepository
                .findAllByUuidInAndWorkspaceUuidAndEnabled(experimentRunUuids, headers.getWorkspaceUuid(), true)
                .stream()
                .collect(Collectors.toMap(ExperimentRun::getUuid, Function.identity()));

        List<String> missingExperimentRunUuids = experimentRunUuids
                .stream()
                .filter(experimentRunUuid -> !experimentRunsByUuid.containsKey(experimentRunUuid))
                .toList();

        if (!missingExperimentRunUuids.isEmpty()) {
            throw new EntityNotFoundException("experiment run uuid", String.join(", ", missingExperimentRunUuids));
        }

        return experimentRunUuids
                .stream()
                .map(experimentRunUuid -> experimentRunTransformer.transformExperimentRunStatusResponseModel(
                        experimentRunsByUuid.get(experimentRunUuid)
                ))
                .toList();
    }

    public Experiment getExperiment(String experimentUuid, ExperimentOpsHeaders headers){
        return experimentRepository
                .findByUuidAndWorkspaceUuidAndStatusAndEnabled(experimentUuid, headers.getWorkspaceUuid(), StatusEnum.ACTIVE, true)
                .orElseThrow(() -> new EntityNotFoundException("experiment", experimentUuid));
    }
}
