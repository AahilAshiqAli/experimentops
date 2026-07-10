package com.experimentops.platformapi.service;

import com.experimentops.common.exceptions.constant.ErrorCode;
import com.experimentops.common.exceptions.runtime.EntityNotFoundException;
import com.experimentops.common.exceptions.runtime.ValidationException;
import com.experimentops.common.kafka.KafkaProducer;
import com.experimentops.experiment.run.event.*;
import com.experimentops.experiment.run.model.v1.ExperimentRunExecutionModeModel;
import com.experimentops.experiment.run.model.v1.ExperimentRunRequestModel;
import com.experimentops.experiment.run.model.v1.ExperimentRunResponseModel;
import com.experimentops.platformapi.dal.repository.DatasetVersionRepository;
import com.experimentops.platformapi.dal.repository.ExperimentConfigRepository;
import com.experimentops.platformapi.dal.repository.ExperimentRepository;
import com.experimentops.platformapi.dal.repository.ExperimentRunRepository;
import com.experimentops.platformapi.dal.repository.RunDatasetRepository;
import com.experimentops.platformapi.model.ExperimentRunConfigContext;
import com.experimentops.platformapi.model.ExperimentRunExecutionConfig;
import com.experimentops.platformapi.model.entity.*;
import com.experimentops.platformapi.model.type.ExperimentStatusEnum;
import com.experimentops.platformapi.model.type.StatusEnum;
import com.experimentops.platformapi.transformer.ExperimentRunTransformer;
import com.experimentops.platformapi.validator.ExperimentRunValidator;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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

    @Value("${experiment.run.requested.topic}")
    private String experimentRunRequestTopic;

    @NonNull
    public ExperimentRunResponseModel publishExperimentRunRequest(@NonNull String experimentUuid, @NonNull ExperimentRunRequestModel experimentRunRequestModel, @NonNull ExperimentOpsHeaders headers){
        experimentRunValidator.validateExperimentRunRequestModel(experimentUuid, experimentRunRequestModel);
        Experiment experiment = experimentRepository
                .findByUuidAndWorkspaceUuidAndEnabled(experimentUuid, headers.getWorkspaceUuid(), true)
                .orElseThrow(() -> new EntityNotFoundException("experiment", experimentUuid));
        List<ExperimentRunExecutionModeModel> requestedExecutionMode = experimentRunRequestModel.getExecutionMode()
                .stream()
                .sorted(Comparator.comparing(ExperimentRunExecutionModeModel::getStepCount))
                .toList();
        DatasetVersion datasetVersion = datasetVersionRepository
                .findByUuidAndWorkspaceUuidAndEnabled(
                        experimentRunRequestModel.getDatasetVersionUuid(),
                        headers.getWorkspaceUuid(),
                        true
                )
                .orElseThrow(() -> new EntityNotFoundException(
                        "dataset version",
                        experimentRunRequestModel.getDatasetVersionUuid()
                ));

        Map<String, ExperimentRunConfigContext> experimentConfigsByUuid = getExperimentConfigsByUuid(
                experimentUuid,
                requestedExecutionMode,
                headers
        );
        experimentRunValidator.validateInputOutputForExecutionMode(
                requestedExecutionMode,
                datasetVersion.getFormat(),
                experimentConfigsByUuid
        );
        List<ExecutionMode> executionMode = experimentRunTransformer.transformExecutionMode(requestedExecutionMode);
        List<ExperimentRunExecutionConfig> executionConfigs = experimentRunTransformer.transformExperimentRunExecutionConfigs(
                requestedExecutionMode,
                experimentConfigsByUuid
        );

        String datasetUri = datasetVersion.getStorageUri();

        ExperimentRun experimentRun = experimentRunTransformer.transformExperimentRunEntity(experimentUuid, executionMode, headers);

        log.info(headers, "saving experiment run object");
        experimentRunRepository.save(experimentRun);

        RunDataset runDataset = experimentRunTransformer.transformRunDatasetEntity(experimentRun, datasetVersion, headers);

        log.info(headers, "saving run dataset object");
        runDatasetRepository.save(runDataset);

        ExperimentRunEvent experimentRunEvent = experimentRunTransformer.transformExperimentRunEvent(
                experiment,
                datasetUri,
                executionConfigs,
                experimentRun.getUuid(),
                headers
        );
        kafkaProducer.sendMessage(experimentRunRequestTopic, experimentRunEvent, experimentRunEvent.getMetadata());
        return experimentRunTransformer.transformExperimentRunResponseModelFromEntity(experimentRun, runDataset.getUuid(), headers);
    }

    public void validateExperimentRunRequest(@NonNull String experimentUuid, @NonNull ExperimentRunRequestModel experimentRunRequestModel, @NonNull ExperimentOpsHeaders headers) {
        experimentRunValidator.validateExperimentRunRequestModel(experimentUuid, experimentRunRequestModel);
        experimentRepository
                .findByUuidAndWorkspaceUuidAndEnabled(experimentUuid, headers.getWorkspaceUuid(), true)
                .orElseThrow(() -> new EntityNotFoundException("experiment", experimentUuid));
        List<ExperimentRunExecutionModeModel> requestedExecutionMode = experimentRunRequestModel.getExecutionMode()
                .stream()
                .sorted(Comparator.comparing(ExperimentRunExecutionModeModel::getStepCount))
                .toList();
        DatasetVersion datasetVersion = datasetVersionRepository
                .findByUuidAndWorkspaceUuidAndEnabled(
                        experimentRunRequestModel.getDatasetVersionUuid(),
                        headers.getWorkspaceUuid(),
                        true
                )
                .orElseThrow(() -> new EntityNotFoundException(
                        "dataset version",
                        experimentRunRequestModel.getDatasetVersionUuid()
                ));
        Map<String, ExperimentRunConfigContext> experimentConfigsByUuid = getExperimentConfigsByUuid(
                experimentUuid,
                requestedExecutionMode,
                headers
        );
        experimentRunValidator.validateInputOutputForExecutionMode(
                requestedExecutionMode,
                datasetVersion.getFormat(),
                experimentConfigsByUuid
        );
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

    public void processExperimentRunCompleted(@NonNull ExperimentRunCompletedEvent event, @NonNull ExperimentOpsHeaders headers) {
        updateExperimentRunStatus(event.getMetadata().getUuid(), headers, ExperimentStatusEnum.SUCCEEDED);
        runArtifactService.uploadArtifacts(event, headers);

    }

    public void processExperimentRunFailure(@NonNull ExperimentRunFailureEvent event, @NonNull ExperimentOpsHeaders headers) {
        updateExperimentRunStatus(event.getMetadata().getUuid(), headers, ExperimentStatusEnum.FAILED);
    }

    private void updateExperimentRunStatus(
            @NonNull String experimentRunUuid,
            @NonNull ExperimentOpsHeaders headers,
            @NonNull ExperimentStatusEnum status) {

        ExperimentRun experimentRun = experimentRunRepository
                .findByUuidAndWorkspaceUuidAndEnabled(experimentRunUuid, headers.getWorkspaceUuid(), true)
                .orElseThrow(() -> new EntityNotFoundException("experiment run uuid", experimentRunUuid));

        log.info(headers, "updating experiment run status");
        experimentRun.setExperimentStatus(status);
        experimentRunRepository.save(experimentRun);
    }

    public void processExperimentRunProgress(@NonNull ExperimentRunProgressEvent event, @NonNull ExperimentOpsHeaders headers) {
        String experimentRunUuid = event.getPayload().getExperimentRunUuid();
        int progress = Integer.parseInt(event.getPayload().getProgress());
        experimentRunRepository.findByUuidAndWorkspaceUuidAndEnabled(experimentRunUuid, headers.getWorkspaceUuid(), true)
                .ifPresentOrElse(
                        experimentRun -> {
                            experimentRun.setProgress(progress);
                            experimentRunRepository.save(experimentRun);
                        },
                        () -> {
                            throw new EntityNotFoundException("Experiment Run uuid", experimentRunUuid);
                        }
                );
    }
}
