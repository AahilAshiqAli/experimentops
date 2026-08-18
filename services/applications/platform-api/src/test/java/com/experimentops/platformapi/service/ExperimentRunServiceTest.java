package com.experimentops.platformapi.service;

import com.experimentops.common.exceptions.runtime.EntityNotFoundException;
import com.experimentops.common.exceptions.constant.ErrorCode;
import com.experimentops.common.exceptions.runtime.ValidationException;
import com.experimentops.common.kafka.KafkaProducer;
import com.experimentops.common.kafka.model.event.ExperimentOpsMetadataEvent;
import com.experimentops.experiment.run.event.ExperimentRunCompletedEvent;
import com.experimentops.experiment.run.event.ExperimentRunCompletedEventPayload;
import com.experimentops.experiment.run.event.ExperimentRunCompletedResult;
import com.experimentops.experiment.run.event.ExperimentRunFailureErrorEntry;
import com.experimentops.experiment.run.event.ExperimentRunFailureEvent;
import com.experimentops.experiment.run.event.ExperimentRunFailureEventPayload;
import com.experimentops.experiment.run.event.ExperimentRunProgressEvent;
import com.experimentops.experiment.run.event.ExperimentRunProgressEventPayload;
import com.experimentops.experiment.run.event.ExperimentRunUserLogEvent;
import com.experimentops.experiment.run.model.v1.ExperimentRunCompareRequestModel;
import com.experimentops.experiment.run.model.v1.ExperimentRunCompareResponseModel;
import com.experimentops.experiment.run.model.v1.ExperimentRunDetailResponseModel;
import com.experimentops.experiment.run.model.v1.ExperimentRunExecutionModeModel;
import com.experimentops.experiment.run.model.v1.ExperimentRunInputModel;
import com.experimentops.experiment.run.model.v1.ExperimentRunRequestModel;
import com.experimentops.objectstorage.gateway.ObjectStorageGateway;
import com.experimentops.platformapi.dal.repository.DatasetVersionRepository;
import com.experimentops.platformapi.dal.repository.ExperimentConfigRepository;
import com.experimentops.platformapi.dal.repository.ExperimentRepository;
import com.experimentops.platformapi.dal.repository.ExperimentRunLogRepository;
import com.experimentops.platformapi.dal.repository.ExperimentRunRepository;
import com.experimentops.platformapi.dal.repository.RunArtifactRepository;
import com.experimentops.platformapi.dal.repository.RunDatasetRepository;
import com.experimentops.platformapi.model.ExperimentRunConfigContext;
import com.experimentops.platformapi.model.ExperimentRunDetailArtifact;
import com.experimentops.platformapi.model.ExperimentRunDetailConfigType;
import com.experimentops.platformapi.model.ExperimentRunDetailDataset;
import com.experimentops.platformapi.model.ExperimentRunDetailSummary;
import com.experimentops.platformapi.model.ExperimentRunResolvedPlan;
import com.experimentops.platformapi.model.entity.DatasetVersion;
import com.experimentops.platformapi.model.entity.ArtifactType;
import com.experimentops.platformapi.model.entity.DownStreamPolicyEnum;
import com.experimentops.platformapi.model.entity.ExecutionMode;
import com.experimentops.platformapi.model.entity.ExecutionModeInput;
import com.experimentops.platformapi.model.entity.Experiment;
import com.experimentops.platformapi.model.entity.ExperimentRun;
import com.experimentops.platformapi.model.entity.ExperimentRunLog;
import com.experimentops.platformapi.model.entity.ExperimentTypeManifest;
import com.experimentops.platformapi.model.entity.FormatStrategy;
import com.experimentops.platformapi.model.entity.FormatStrategyTypeEnum;
import com.experimentops.platformapi.model.entity.OutputDataKindEnum;
import com.experimentops.platformapi.model.entity.OutputManifest;
import com.experimentops.platformapi.model.entity.RunArtifact;
import com.experimentops.platformapi.model.entity.RunArtifactStatusEnum;
import com.experimentops.platformapi.model.entity.RunDataset;
import com.experimentops.platformapi.model.type.DatasetFileFormatEnum;
import com.experimentops.platformapi.model.type.DatasetScanStatusEnum;
import com.experimentops.platformapi.model.type.ExperimentStatusEnum;
import com.experimentops.platformapi.model.type.StatusEnum;
import com.experimentops.platformapi.transformer.ExperimentRunTransformer;
import com.experimentops.platformapi.validator.ExperimentRunValidator;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExperimentRunServiceTest {

    @Test
    void publishExperimentRunRequestSavesRunDatasetWithStepAndPortForEachDatasetAttachment() {
        KafkaProducer kafkaProducer = mock(KafkaProducer.class);
        ExperimentRepository experimentRepository = mock(ExperimentRepository.class);
        ExperimentConfigRepository experimentConfigRepository = mock(ExperimentConfigRepository.class);
        ExperimentRunRepository experimentRunRepository = mock(ExperimentRunRepository.class);
        DatasetVersionRepository datasetVersionRepository = mock(DatasetVersionRepository.class);
        RunDatasetRepository runDatasetRepository = mock(RunDatasetRepository.class);
        ExperimentRunService service = new ExperimentRunService(
                kafkaProducer,
                new ExperimentRunValidator(),
                new ExperimentRunTransformer(),
                experimentRepository,
                experimentConfigRepository,
                experimentRunRepository,
                datasetVersionRepository,
                runDatasetRepository,
                null,
                null,
                null,
                null
        );
        ExperimentOpsHeaders headers = headers();
        Experiment experiment = Experiment.builder()
                .workspaceUuid("workspace-1")
                .status(StatusEnum.ACTIVE)
                .build();
        experiment.setUuid("experiment-1");
        DatasetVersion datasetVersion = DatasetVersion.builder()
                .format("CSV")
                .scanStatus(DatasetScanStatusEnum.COMPLETED)
                .status(StatusEnum.ACTIVE)
                .storageUri("s3://bucket/input.csv")
                .build();
        datasetVersion.setUuid("dataset-version-1");
        com.experimentops.platformapi.dal.repository.ExperimentConfigWithTypeProjection projection =
                projection("config-1", "CSV_PROFILE_ANALYSIS_NORMALIZE");
        when(projection.getFormatMappings()).thenReturn("""
                [{
                  "inputs": [
                    {
                      "portName": "trainingData",
                      "required": true,
                      "contract": {
                        "dataKind": "TABULAR_DATASET",
                        "acceptedFormats": ["CSV"]
                      }
                    },
                    {
                      "portName": "validationData",
                      "required": true,
                      "contract": {
                        "dataKind": "TABULAR_DATASET",
                        "acceptedFormats": ["CSV"]
                      }
                    }
                  ],
                  "outputs": [
                    {
                      "name": "report",
                      "required": true,
                      "dataKind": "REPORT",
                      "type": {
                        "type": "FIXED",
                        "format": "JSON"
                      },
                      "downStreamPolicy": "TERMINAL"
                    }
                  ]
                }]
                """);
        ExperimentRunRequestModel request = new ExperimentRunRequestModel()
                .name("run-1")
                .executionMode(List.of(new ExperimentRunExecutionModeModel()
                        .stepCount(1)
                        .experimentConfigUuid("config-1")
                        .inputs(List.of(
                                new ExperimentRunInputModel()
                                        .portName("trainingData")
                                        .inputType(ExperimentRunInputModel.InputTypeEnum.DATASET)
                                        .file("dataset-version-1"),
                                new ExperimentRunInputModel()
                                        .portName("validationData")
                                        .inputType(ExperimentRunInputModel.InputTypeEnum.DATASET)
                                        .file("dataset-version-1")
                        ))));

        when(experimentRepository.findByUuidAndWorkspaceUuidAndStatusAndEnabled(
                "experiment-1",
                "workspace-1",
                StatusEnum.ACTIVE,
                true
        )).thenReturn(Optional.of(experiment));
        when(datasetVersionRepository.findAllByUuidInAndWorkspaceUuidAndStatusAndEnabled(
                List.of("dataset-version-1"),
                "workspace-1",
                StatusEnum.ACTIVE,
                true
        )).thenReturn(List.of(datasetVersion));
        when(experimentConfigRepository.findAllWithExperimentTypesByUuidIn(
                anyCollection(),
                eq("experiment-1"),
                eq("workspace-1"),
                eq(StatusEnum.ACTIVE.getCode())
        )).thenReturn(List.of(projection));

        service.publishExperimentRunRequest("experiment-1", request, headers);

        ArgumentCaptor<List<RunDataset>> runDatasetsCaptor = ArgumentCaptor.forClass(List.class);
        verify(runDatasetRepository).saveAll(runDatasetsCaptor.capture());
        assertThat(runDatasetsCaptor.getValue())
                .extracting(
                        RunDataset::getDatasetVersionUuid,
                        RunDataset::getStepCount,
                        RunDataset::getPortName
                )
                .containsExactly(
                        org.assertj.core.api.Assertions.tuple("dataset-version-1", 1, "trainingData"),
                        org.assertj.core.api.Assertions.tuple("dataset-version-1", 1, "validationData")
                );
    }

    @Test
    void getExperimentRequiresActiveExperiment() {
        ExperimentRepository experimentRepository = mock(ExperimentRepository.class);
        ExperimentRunService service = new ExperimentRunService(
                null,
                new ExperimentRunValidator(),
                null,
                experimentRepository,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        ExperimentOpsHeaders headers = new ExperimentOpsHeaders();
        headers.setWorkspaceUuid("workspace-1");

        when(experimentRepository.findByUuidAndWorkspaceUuidAndStatusAndEnabled(
                "experiment-1",
                "workspace-1",
                StatusEnum.ACTIVE,
                true
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getExperiment("experiment-1", headers))
                .isInstanceOf(EntityNotFoundException.class);

        verify(experimentRepository).findByUuidAndWorkspaceUuidAndStatusAndEnabled(
                "experiment-1",
                "workspace-1",
                StatusEnum.ACTIVE,
                true
        );
    }

    @Test
    void completionMarksRunFailedWhenExperimentConfigIsMissingOrInactive() {
        ExperimentRunRepository experimentRunRepository = mock(ExperimentRunRepository.class);
        ExperimentConfigRepository experimentConfigRepository = mock(ExperimentConfigRepository.class);
        ExperimentRunTransformer experimentRunTransformer = mock(ExperimentRunTransformer.class);
        ExperimentRunService service = new ExperimentRunService(
                null,
                new ExperimentRunValidator(),
                experimentRunTransformer,
                null,
                experimentConfigRepository,
                experimentRunRepository,
                mock(DatasetVersionRepository.class),
                null,
                null,
                null,
                null,
                null
        );
        ExperimentOpsHeaders headers = headers();
        ExperimentRun experimentRun = experimentRun(
                List.of(ExecutionMode.builder()
                        .stepCount(1)
                        .experimentConfigUuid("config-1")
                        .inputs(List.of())
                        .build())
        );

        when(experimentRunRepository.findByUuidAndWorkspaceUuidAndEnabled("run-1", "workspace-1", true))
                .thenReturn(Optional.of(experimentRun));
        when(experimentRunTransformer.transformExecutionModeModel(experimentRun.getExecutionMode()))
                .thenReturn(List.of(new ExperimentRunExecutionModeModel()
                        .stepCount(1)
                        .experimentConfigUuid("config-1")
                        .inputs(List.of())));
        when(experimentConfigRepository.findAllWithExperimentTypesByUuidIn(
                anyCollection(),
                eq("experiment-1"),
                eq("workspace-1"),
                eq(StatusEnum.ACTIVE.getCode())
        )).thenReturn(List.of());

        service.processExperimentRunCompleted(completedEvent(), headers);

        assertThat(experimentRun.getExperimentStatus()).isEqualTo(ExperimentStatusEnum.FAILED);
        assertThat(experimentRun.getMessage()).isEqualTo(
                "Experiment config config-1 failed step number : 1. Experiment config not found or inactive"
        );
        assertThat(experimentRun.getLogs()).isEqualTo("s3://bucket/run.log");
        verify(experimentRunRepository).save(experimentRun);
    }

    @Test
    void completionMarksRunFailedWhenCompletedArtifactsAreInvalid() {
        ExperimentRunRepository experimentRunRepository = mock(ExperimentRunRepository.class);
        ExperimentConfigRepository experimentConfigRepository = mock(ExperimentConfigRepository.class);
        DatasetVersionRepository datasetVersionRepository = mock(DatasetVersionRepository.class);
        ExperimentRunValidator experimentRunValidator = mock(ExperimentRunValidator.class);
        ExperimentRunTransformer experimentRunTransformer = mock(ExperimentRunTransformer.class);
        RunArtifactService runArtifactService = mock(RunArtifactService.class);
        ExperimentRunService service = new ExperimentRunService(
                null,
                experimentRunValidator,
                experimentRunTransformer,
                null,
                experimentConfigRepository,
                experimentRunRepository,
                datasetVersionRepository,
                null,
                runArtifactService,
                null,
                null,
                null
        );
        ExperimentOpsHeaders headers = headers();
        ExperimentRun experimentRun = experimentRun(List.of());
        ExperimentRunExecutionModeModel executionMode = new ExperimentRunExecutionModeModel()
                .stepCount(1)
                .experimentConfigUuid("config-1")
                .inputs(List.of(new ExperimentRunInputModel()
                        .inputType(ExperimentRunInputModel.InputTypeEnum.DATASET)
                        .file("dataset-version-1")));
        DatasetVersion datasetVersion = DatasetVersion.builder()
                .format("CSV")
                .scanStatus(DatasetScanStatusEnum.COMPLETED)
                .status(StatusEnum.ACTIVE)
                .storageUri("s3://bucket/input.csv")
                .build();
        datasetVersion.setUuid("dataset-version-1");
        ExperimentRunConfigContext config = ExperimentRunConfigContext.builder()
                .experimentConfigUuid("config-1")
                .experimentConfigName("CSV_PROFILE_ANALYSIS_NORMALIZE")
                .experimentType("CSV_PROFILE_ANALYSIS")
                .formatMappings(List.of(ExperimentTypeManifest.builder()
                        .outputs(List.of(OutputManifest.builder()
                                .name("report")
                                .required(true)
                                .dataKind(OutputDataKindEnum.REPORT)
                                .type(FormatStrategy.builder()
                                        .type(FormatStrategyTypeEnum.FIXED)
                                        .format(DatasetFileFormatEnum.JSON)
                                        .build())
                                .downStreamPolicy(DownStreamPolicyEnum.TERMINAL)
                                .build()))
                        .build()))
                .build();
        ExperimentRunResolvedPlan resolvedPlan = ExperimentRunResolvedPlan.builder().steps(List.of()).build();
        String failureMessage = "CSV_PROFILE_ANALYSIS_NORMALIZE failed step number : 1. Did not produce required artifact: report";
        com.experimentops.platformapi.dal.repository.ExperimentConfigWithTypeProjection projection =
                projection("config-1", "CSV_PROFILE_ANALYSIS_NORMALIZE");

        when(experimentRunRepository.findByUuidAndWorkspaceUuidAndEnabled("run-1", "workspace-1", true))
                .thenReturn(Optional.of(experimentRun));
        when(experimentRunTransformer.transformExecutionModeModel(experimentRun.getExecutionMode()))
                .thenReturn(List.of(executionMode));
        when(experimentRunTransformer.transformExperimentRunConfigContext(projection))
                .thenReturn(config);
        when(experimentRunValidator.collectDatasetVersionUuids(List.of(executionMode)))
                .thenReturn(List.of("dataset-version-1"));
        when(datasetVersionRepository.findAllByUuidInAndWorkspaceUuidAndStatusAndEnabled(
                List.of("dataset-version-1"),
                "workspace-1",
                StatusEnum.ACTIVE,
                true
        )).thenReturn(List.of(datasetVersion));
        when(experimentConfigRepository.findAllWithExperimentTypesByUuidIn(
                anyCollection(),
                eq("experiment-1"),
                eq("workspace-1"),
                eq(StatusEnum.ACTIVE.getCode())
        )).thenReturn(List.of(projection));
        when(experimentRunValidator.validateAndResolveExecutionPlan(
                eq(List.of(executionMode)),
                eq(Map.of("config-1", config)),
                eq(Map.of("dataset-version-1", datasetVersion))
        )).thenReturn(resolvedPlan);
        doThrow(new ValidationException(ErrorCode.INVALID_INPUTS, failureMessage))
                .when(experimentRunValidator)
                .validateCompletedArtifacts(any(ExperimentRunCompletedEvent.class), eq(resolvedPlan), eq(Map.of(1, "CSV_PROFILE_ANALYSIS_NORMALIZE")));

        service.processExperimentRunCompleted(completedEvent(), headers);

        assertThat(experimentRun.getExperimentStatus()).isEqualTo(ExperimentStatusEnum.FAILED);
        assertThat(experimentRun.getMessage()).isEqualTo(failureMessage);
        assertThat(experimentRun.getLogs()).isEqualTo("s3://bucket/run.log");
        verify(runArtifactService, never()).uploadArtifacts(any(), any(), any(), any());
        verify(experimentRunRepository).save(experimentRun);
    }

    @Test
    void duplicateCompletionDoesNotCreateArtifactsAgain() {
        ExperimentRunRepository experimentRunRepository = mock(ExperimentRunRepository.class);
        RunArtifactService runArtifactService = mock(RunArtifactService.class);
        ExperimentRunService service = new ExperimentRunService(
                null,
                new ExperimentRunValidator(),
                new ExperimentRunTransformer(),
                null,
                null,
                experimentRunRepository,
                null,
                null,
                runArtifactService,
                null,
                null,
                null
        );
        ExperimentOpsHeaders headers = headers();
        ExperimentRun experimentRun = experimentRun(List.of());
        experimentRun.setExperimentStatus(ExperimentStatusEnum.SUCCEEDED);

        when(experimentRunRepository.findByUuidAndWorkspaceUuidAndEnabled("run-1", "workspace-1", true))
                .thenReturn(Optional.of(experimentRun));

        service.processExperimentRunCompleted(completedEvent(), headers);

        verify(runArtifactService, never()).uploadArtifacts(any(), any(), any(), any());
        verify(experimentRunRepository, never()).save(any());
    }

    @Test
    void failureEventMarksRunFailedWithPayloadErrorMessage() {
        ExperimentRunRepository experimentRunRepository = mock(ExperimentRunRepository.class);
        ExperimentRunService service = new ExperimentRunService(
                null,
                new ExperimentRunValidator(),
                null,
                null,
                null,
                experimentRunRepository,
                null,
                null,
                null,
                null,
                null,
                null
        );
        ExperimentOpsHeaders headers = headers();
        ExperimentRun experimentRun = experimentRun(List.of());

        when(experimentRunRepository.findByUuidAndWorkspaceUuidAndEnabled("run-1", "workspace-1", true))
                .thenReturn(Optional.of(experimentRun));

        service.processExperimentRunFailure(failureEvent(), headers);

        assertThat(experimentRun.getExperimentStatus()).isEqualTo(ExperimentStatusEnum.FAILED);
        assertThat(experimentRun.getMessage()).isEqualTo("RuntimeError: worker failed");
        assertThat(experimentRun.getLogs()).isEqualTo("s3://bucket/run.log");
        verify(experimentRunRepository).save(experimentRun);
    }

    @Test
    void failureEventDoesNotOverwriteLogsWhenLogFileUrlIsBlank() {
        ExperimentRunRepository experimentRunRepository = mock(ExperimentRunRepository.class);
        ExperimentRunService service = new ExperimentRunService(
                null,
                new ExperimentRunValidator(),
                null,
                null,
                null,
                experimentRunRepository,
                null,
                null,
                null,
                null,
                null,
                null
        );
        ExperimentOpsHeaders headers = headers();
        ExperimentRun experimentRun = experimentRun(List.of());
        experimentRun.setLogs("s3://bucket/existing.log");
        ExperimentRunFailureEvent event = new ExperimentRunFailureEvent(
                metadata("EXPERIMENT_RUN_FAILURE"),
                new ExperimentRunFailureEventPayload(
                        "project-1",
                        "experiment-1",
                        "CSV_PROFILE_ANALYSIS",
                        "FAILED",
                        List.of(new ExperimentRunFailureErrorEntry(
                                "RuntimeError",
                                "worker failed",
                                "stack"
                        )),
                        "   "
                )
        );

        when(experimentRunRepository.findByUuidAndWorkspaceUuidAndEnabled("run-1", "workspace-1", true))
                .thenReturn(Optional.of(experimentRun));

        service.processExperimentRunFailure(event, headers);

        assertThat(experimentRun.getLogs()).isEqualTo("s3://bucket/existing.log");
        verify(experimentRunRepository).save(experimentRun);
    }

    @Test
    void progressEventUpdatesProgressAndSavesLogs() {
        ExperimentRunRepository experimentRunRepository = mock(ExperimentRunRepository.class);
        ExperimentRunLogRepository experimentRunLogRepository = mock(ExperimentRunLogRepository.class);
        ExperimentRunService service = new ExperimentRunService(
                null,
                new ExperimentRunValidator(),
                new ExperimentRunTransformer(),
                null,
                null,
                experimentRunRepository,
                null,
                null,
                null,
                null,
                experimentRunLogRepository,
                null
        );
        ExperimentOpsHeaders headers = headers();
        ExperimentRun experimentRun = experimentRun(List.of());
        Instant timestamp = Instant.parse("2026-01-01T00:00:00Z");
        ExperimentRunProgressEvent event = new ExperimentRunProgressEvent(
                metadata("EXPERIMENT_RUN_PROGRESS"),
                new ExperimentRunProgressEventPayload(
                        "experiment-1",
                        "run-1",
                        "42",
                        1,
                        7L,
                        List.of(new ExperimentRunUserLogEvent(
                                timestamp,
                                "INFO",
                                "started step",
                                "CSV_PROFILE_ANALYSIS"
                        ))
                )
        );

        when(experimentRunRepository.findByUuidAndWorkspaceUuidAndEnabled("run-1", "workspace-1", true))
                .thenReturn(Optional.of(experimentRun));

        service.processExperimentRunProgress(event, headers);

        assertThat(experimentRun.getProgress()).isEqualTo(42);
        assertThat(experimentRun.getStepCompleted()).isEqualTo(1);
        verify(experimentRunRepository).save(experimentRun);
        ArgumentCaptor<List<ExperimentRunLog>> logsCaptor = ArgumentCaptor.forClass(List.class);
        verify(experimentRunLogRepository).saveAll(logsCaptor.capture());
        assertThat(logsCaptor.getValue()).singleElement()
                .satisfies(log -> {
                    assertThat(log.getExperimentRunUuid()).isEqualTo("run-1");
                    assertThat(log.getSequence()).isEqualTo(7L);
                    assertThat(log.getTimestamp()).isEqualTo(Timestamp.from(timestamp));
                    assertThat(log.getLevel()).isEqualTo("INFO");
                    assertThat(log.getMessage()).isEqualTo("started step");
                    assertThat(log.getExperimentType()).isEqualTo("CSV_PROFILE_ANALYSIS");
                    assertThat(log.getCreatedBy()).isEqualTo("user-1");
                    assertThat(log.getUpdatedBy()).isEqualTo("user-1");
                });
    }

    @Test
    void getExperimentRunReturnsDetailResponse() {
        ExperimentRunRepository experimentRunRepository = mock(ExperimentRunRepository.class);
        DatasetVersionRepository datasetVersionRepository = mock(DatasetVersionRepository.class);
        RunArtifactRepository runArtifactRepository = mock(RunArtifactRepository.class);
        ExperimentRunService service = new ExperimentRunService(
                null,
                new ExperimentRunValidator(),
                new ExperimentRunTransformer(),
                null,
                null,
                experimentRunRepository,
                datasetVersionRepository,
                null,
                null,
                runArtifactRepository,
                null,
                null
        );
        ExperimentOpsHeaders headers = headers();
        ExecutionMode executionModeStep = ExecutionMode.builder()
                .stepCount(1)
                .experimentConfigUuid("config-1")
                .inputs(List.of(
                        ExecutionModeInput.builder()
                                .portName("trainData")
                                .inputType("DATASET")
                                .file("dataset-version-1")
                                .build(),
                        ExecutionModeInput.builder()
                                .portName("testData")
                                .inputType("DATASET")
                                .file("dataset-version-1")
                                .build(),
                        ExecutionModeInput.builder()
                                .portName("cleanedData")
                                .inputType("ARTIFACT")
                                .file("cleanedData")
                                .sourceStepCount(1)
                                .build()
                ))
                .build();
        ExperimentRunDetailSummary summary = new ExperimentRunDetailSummary(
                "run-1",
                "Run 1",
                ExperimentStatusEnum.RUNNING,
                "Running",
                "experiment-1",
                "Experiment 1",
                "project-1",
                "Project 1",
                Timestamp.valueOf("2026-01-01 00:00:00"),
                Timestamp.valueOf("2026-01-01 00:01:00"),
                "user-1",
                1L,
                1,
                List.of(executionModeStep)
        );

        when(experimentRunRepository.findExperimentRunDetailSummary("run-1", "workspace-1"))
                .thenReturn(Optional.of(summary));
        when(experimentRunRepository.findExperimentRunDetailDatasets("run-1", "workspace-1"))
                .thenReturn(List.of(new ExperimentRunDetailDataset("dataset-version-1", "customers.csv")));
        when(experimentRunRepository.findExperimentRunDetailPrimaryArtifacts("run-1", "workspace-1"))
                .thenReturn(List.of(new ExperimentRunDetailArtifact("artifact-1", "report")));
        when(experimentRunRepository.findExperimentRunDetailConfigTypes(
                List.of("config-1"),
                "experiment-1",
                "workspace-1"
        )).thenReturn(List.of(new ExperimentRunDetailConfigType("config-1", "CSV_PROFILE_ANALYSIS")));
        DatasetVersion datasetVersion = DatasetVersion.builder()
                .name("customers.csv")
                .format("CSV")
                .status(StatusEnum.ACTIVE)
                .build();
        datasetVersion.setUuid("dataset-version-1");
        RunArtifact runArtifact = RunArtifact.builder()
                .experimentRunUuid("run-1")
                .workspaceUuid("workspace-1")
                .artifactType(ArtifactType.TABULAR_DATASET)
                .format("CSV")
                .size(100L)
                .stepCount(1)
                .portName("cleanedData")
                .status(RunArtifactStatusEnum.PRIMARY)
                .downStreamPolicy(DownStreamPolicyEnum.CONNECTABLE)
                .build();
        runArtifact.setUuid("artifact-1");
        when(datasetVersionRepository.findAllByUuidInAndWorkspaceUuidAndStatusAndEnabled(
                List.of("dataset-version-1"),
                "workspace-1",
                StatusEnum.ACTIVE,
                true
        )).thenReturn(List.of(datasetVersion));
        when(runArtifactRepository.findByExperimentRunUuidAndWorkspaceUuidAndEnabledOrderByStepCountAscPortNameAsc(
                "run-1",
                "workspace-1",
                true
        )).thenReturn(List.of(runArtifact));

        ExperimentRunDetailResponseModel response = service.getExperimentRun("run-1", headers);

        assertThat(response.getUuid()).isEqualTo("run-1");
        assertThat(response.getName()).isEqualTo("Run 1");
        assertThat(response.getStatus()).isEqualTo("RUNNING");
        assertThat(response.getMessage()).isEqualTo("Running");
        assertThat(response.getExperimentUUID()).isEqualTo("experiment-1");
        assertThat(response.getExperimentName()).isEqualTo("Experiment 1");
        assertThat(response.getProjectUUID()).isEqualTo("project-1");
        assertThat(response.getProjectName()).isEqualTo("Project 1");
        assertThat(response.getCreatedBy()).isEqualTo("user-1");
        assertThat(response.getArtifactCount()).isEqualTo(1L);
        assertThat(response.getCompletedSteps()).isEqualTo(1);
        assertThat(response.getNumSteps()).isEqualTo(1);
        assertThat(response.getDatasets()).singleElement()
                .satisfies(dataset -> {
                    assertThat(dataset.getDatasetVersionUuid()).isEqualTo("dataset-version-1");
                    assertThat(dataset.getName()).isEqualTo("customers.csv");
                });
        assertThat(response.getRunArtifacts()).singleElement()
                .satisfies(artifact -> {
                    assertThat(artifact.getUuid()).isEqualTo("artifact-1");
                    assertThat(artifact.getPortName()).isEqualTo("report");
                });
        assertThat(response.getExecutionMode()).singleElement()
                .satisfies(executionMode -> {
                    assertThat(executionMode.getExperimentConfigUuid()).isEqualTo("config-1");
                    assertThat(executionMode.getExperimentType()).isEqualTo("CSV_PROFILE_ANALYSIS");
                    assertThat(executionMode.getInputs()).hasSize(3);
                    assertThat(executionMode.getInputs().get(0).getPortName()).isEqualTo("trainData");
                    assertThat(executionMode.getInputs().get(0).getInputType().getValue()).isEqualTo("DATASET");
                    assertThat(executionMode.getInputs().get(0).getName()).isEqualTo("customers.csv");
                    assertThat(executionMode.getInputs().get(0).getFileUuid()).isEqualTo("dataset-version-1");
                    assertThat(executionMode.getInputs().get(0).getFormat()).isEqualTo("CSV");
                    assertThat(executionMode.getInputs().get(1).getPortName()).isEqualTo("testData");
                    assertThat(executionMode.getInputs().get(1).getInputType().getValue()).isEqualTo("DATASET");
                    assertThat(executionMode.getInputs().get(1).getName()).isEqualTo("customers.csv");
                    assertThat(executionMode.getInputs().get(1).getFileUuid()).isEqualTo("dataset-version-1");
                    assertThat(executionMode.getInputs().get(1).getFormat()).isEqualTo("CSV");
                    assertThat(executionMode.getInputs().get(2).getPortName()).isEqualTo("cleanedData");
                    assertThat(executionMode.getInputs().get(2).getInputType().getValue()).isEqualTo("ARTIFACT");
                    assertThat(executionMode.getInputs().get(2).getName()).isEqualTo("cleanedData.csv");
                    assertThat(executionMode.getInputs().get(2).getFileUuid()).isEqualTo("artifact-1");
                    assertThat(executionMode.getInputs().get(2).getFormat()).isEqualTo("CSV");
                    assertThat(executionMode.getOutputs()).singleElement()
                            .satisfies(output -> {
                                assertThat(output.getArtifactUuid()).isEqualTo("artifact-1");
                                assertThat(output.getPortName()).isEqualTo("cleanedData");
                                assertThat(output.getName()).isEqualTo("cleanedData.csv");
                                assertThat(output.getFormat()).isEqualTo("CSV");
                                assertThat(output.getSize()).isEqualTo(100L);
                                assertThat(output.getStatus()).isEqualTo("PRIMARY");
                                assertThat(output.getDownstreamPolicy()).isEqualTo("CONNECTABLE");
                            });
                });
        verify(datasetVersionRepository, times(1)).findAllByUuidInAndWorkspaceUuidAndStatusAndEnabled(
                List.of("dataset-version-1"),
                "workspace-1",
                StatusEnum.ACTIVE,
                true
        );
    }

    @Test
    void compareExperimentReturnsReportsInRequestedRunOrder() {
        ExperimentRunRepository experimentRunRepository = mock(ExperimentRunRepository.class);
        RunArtifactRepository runArtifactRepository = mock(RunArtifactRepository.class);
        ObjectStorageGateway objectStorageGateway = mock(ObjectStorageGateway.class);
        ExperimentRun firstRun = successfulComparisonRun("run-1", "config-1");
        ExperimentRun secondRun = successfulComparisonRun("run-2", "config-2");
        RunArtifact firstReport = comparisonReportArtifact("run-1", "s3://bucket/run-1/report.json");
        RunArtifact secondReport = comparisonReportArtifact("run-2", "s3://bucket/run-2/report.json");
        ExperimentRunService service = new ExperimentRunService(
                null,
                new ExperimentRunValidator(),
                new ExperimentRunTransformer(),
                null,
                null,
                experimentRunRepository,
                null,
                null,
                null,
                runArtifactRepository,
                null,
                objectStorageGateway
        );

        when(experimentRunRepository.findAllByUuidInAndWorkspaceUuidAndEnabled(
                List.of("run-1", "run-2"),
                "workspace-1",
                true
        )).thenReturn(List.of(secondRun, firstRun));
        when(runArtifactRepository.findByExperimentRunUuidAndWorkspaceUuidAndArtifactTypeAndStatusAndEnabled(
                "run-1", "workspace-1", ArtifactType.REPORT, RunArtifactStatusEnum.PRIMARY, true
        )).thenReturn(List.of(firstReport));
        when(runArtifactRepository.findByExperimentRunUuidAndWorkspaceUuidAndArtifactTypeAndStatusAndEnabled(
                "run-2", "workspace-1", ArtifactType.REPORT, RunArtifactStatusEnum.PRIMARY, true
        )).thenReturn(List.of(secondReport));
        when(objectStorageGateway.downloadObject("s3://bucket/run-1/report.json"))
                .thenReturn("{\"schemaVersion\":1,\"metrics\":{\"accuracy\":0.91}}".getBytes(StandardCharsets.UTF_8));
        when(objectStorageGateway.downloadObject("s3://bucket/run-2/report.json"))
                .thenReturn("{\"schemaVersion\":1,\"metrics\":{\"accuracy\":0.94}}".getBytes(StandardCharsets.UTF_8));

        ExperimentRunCompareResponseModel response = service.compareExperiment(
                new ExperimentRunCompareRequestModel(
                        List.of("run-1", "run-2"),
                        ExperimentRunCompareRequestModel.ComparisonAxisEnum.CONFIG
                ),
                headers()
        );

        assertThat(response.getComparisonAxis())
                .isEqualTo(ExperimentRunCompareResponseModel.ComparisonAxisEnum.CONFIG);
        assertThat(response.getPipelineSignature())
                .containsExactly("BINARY_CLASSIFICATION_EVALUATION");
        assertThat(response.getRuns())
                .extracting(report -> report.getExperimentRunUuid())
                .containsExactly("run-1", "run-2");
        assertThat(response.getRuns().getFirst().getEvaluationReport())
                .containsEntry("schemaVersion", 1);
    }

    private static ExperimentOpsHeaders headers() {
        ExperimentOpsHeaders headers = new ExperimentOpsHeaders();
        headers.setWorkspaceUuid("workspace-1");
        headers.setUserUuid("user-1");
        return headers;
    }

    private static ExperimentRun successfulComparisonRun(String uuid, String configUuid) {
        ExperimentRun run = ExperimentRun.builder()
                .experimentUuid("experiment-1")
                .workspaceUuid("workspace-1")
                .experimentStatus(ExperimentStatusEnum.SUCCEEDED)
                .executionMode(List.of(ExecutionMode.builder()
                        .stepCount(1)
                        .experimentType("BINARY_CLASSIFICATION_EVALUATION")
                        .experimentConfigUuid(configUuid)
                        .inputs(List.of(ExecutionModeInput.builder()
                                .portName("testDataset")
                                .inputType("DATASET")
                                .file("dataset-1")
                                .build()))
                        .build()))
                .build();
        run.setUuid(uuid);
        return run;
    }

    private static RunArtifact comparisonReportArtifact(String runUuid, String storageUri) {
        return RunArtifact.builder()
                .experimentRunUuid(runUuid)
                .workspaceUuid("workspace-1")
                .artifactType(ArtifactType.REPORT)
                .storageUri(storageUri)
                .format("JSON")
                .experimentType("BINARY_CLASSIFICATION_EVALUATION")
                .stepCount(1)
                .portName("evaluationReport")
                .status(RunArtifactStatusEnum.PRIMARY)
                .downStreamPolicy(DownStreamPolicyEnum.TERMINAL)
                .build();
    }

    private static ExperimentRun experimentRun(List<ExecutionMode> executionMode) {
        ExperimentRun experimentRun = ExperimentRun.builder()
                .experimentUuid("experiment-1")
                .workspaceUuid("workspace-1")
                .executionMode(executionMode)
                .experimentStatus(ExperimentStatusEnum.RUNNING)
                .build();
        experimentRun.setUuid("run-1");
        return experimentRun;
    }

    private static ExperimentRunCompletedEvent completedEvent() {
        return new ExperimentRunCompletedEvent(
                metadata("EXPERIMENT_RUN_COMPLETED"),
                new ExperimentRunCompletedEventPayload(
                        null,
                        null,
                        null,
                        "SUCCEEDED",
                        new ExperimentRunCompletedResult(List.of(), List.of(), null),
                        "s3://bucket/run.log"
                )
        );
    }

    private static ExperimentRunFailureEvent failureEvent() {
        return new ExperimentRunFailureEvent(
                metadata("EXPERIMENT_RUN_FAILURE"),
                new ExperimentRunFailureEventPayload(
                        "project-1",
                        "experiment-1",
                        "CSV_PROFILE_ANALYSIS",
                        "FAILED",
                        List.of(new ExperimentRunFailureErrorEntry(
                                "RuntimeError",
                                "worker failed",
                                "stack"
                        )),
                        "s3://bucket/run.log"
                )
        );
    }

    private static ExperimentOpsMetadataEvent metadata(String eventType) {
        return ExperimentOpsMetadataEvent.newBuilder()
                .setTraceUuid("request-1")
                .setRequesterUuid("user-1")
                .setEventType(eventType)
                .setEventUuid("event-1")
                .setEventTimestamp(1L)
                .setUuid("run-1")
                .setWorkspaceUuid("workspace-1")
                .setRequestTimestamp(1L)
                .build();
    }

    private static com.experimentops.platformapi.dal.repository.ExperimentConfigWithTypeProjection projection(
            String uuid,
            String name) {

        com.experimentops.platformapi.dal.repository.ExperimentConfigWithTypeProjection projection =
                mock(com.experimentops.platformapi.dal.repository.ExperimentConfigWithTypeProjection.class);
        when(projection.getExperimentConfigUuid()).thenReturn(uuid);
        when(projection.getName()).thenReturn(name);
        when(projection.getExperimentType()).thenReturn("CSV_PROFILE_ANALYSIS");
        when(projection.getFormatMappings()).thenReturn("[]");
        return projection;
    }
}
