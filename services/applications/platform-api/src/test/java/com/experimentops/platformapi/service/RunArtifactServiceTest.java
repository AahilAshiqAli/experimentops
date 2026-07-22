package com.experimentops.platformapi.service;

import com.experimentops.common.kafka.model.event.ExperimentOpsMetadataEvent;
import com.experimentops.experiment.run.event.ExperimentRunCompletedArtifact;
import com.experimentops.experiment.run.event.ExperimentRunCompletedEvent;
import com.experimentops.experiment.run.event.ExperimentRunCompletedEventPayload;
import com.experimentops.experiment.run.event.ExperimentRunCompletedResult;
import com.experimentops.objectstorage.gateway.ObjectStorageGateway;
import com.experimentops.platformapi.dal.repository.RunArtifactRepository;
import com.experimentops.platformapi.model.ExperimentRunResolvedPlan;
import com.experimentops.platformapi.model.entity.DownStreamPolicyEnum;
import com.experimentops.platformapi.model.entity.RunArtifact;
import com.experimentops.platformapi.model.entity.RunArtifactStatusEnum;
import com.experimentops.platformapi.transformer.ExperimentRunTransformer;
import com.experimentops.run.artifact.model.v1.RunArtifactDownloadUrlResponseModel;
import com.experimentops.run.artifact.model.v1.RunArtifactListResponseModel;
import com.experimentops.run.artifact.model.v1.RunArtifactResponseModel;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RunArtifactServiceTest {

    @Test
    void uploadArtifactsMarksOnlyLastStepArtifactsPrimary() {
        RunArtifactRepository runArtifactRepository = mock(RunArtifactRepository.class);
        RunArtifactService service = new RunArtifactService(
                runArtifactRepository,
                new ExperimentRunTransformer(),
                mock(ObjectStorageGateway.class)
        );
        ExperimentOpsHeaders headers = new ExperimentOpsHeaders();
        headers.setWorkspaceUuid("workspace-1");
        headers.setUserUuid("user-1");
        ExperimentRunResolvedPlan resolvedPlan = ExperimentRunResolvedPlan.builder()
                .steps(List.of(
                        ExperimentRunResolvedPlan.Step.builder()
                                .stepCount(1)
                                .outputs(List.of(ExperimentRunResolvedPlan.Output.builder()
                                        .name("normalized")
                                        .downStreamPolicy(DownStreamPolicyEnum.CONNECTABLE)
                                        .build()))
                                .build(),
                        ExperimentRunResolvedPlan.Step.builder()
                                .stepCount(2)
                                .outputs(List.of(ExperimentRunResolvedPlan.Output.builder()
                                        .name("report")
                                        .downStreamPolicy(DownStreamPolicyEnum.TERMINAL)
                                        .build()))
                                .build()
                ))
                .build();

        service.uploadArtifacts(completedEvent(), "run-1", resolvedPlan, headers);

        ArgumentCaptor<List<RunArtifact>> artifactsCaptor = ArgumentCaptor.forClass(List.class);
        verify(runArtifactRepository).saveAll(artifactsCaptor.capture());
        assertThat(artifactsCaptor.getValue())
                .extracting(RunArtifact::getPortName, RunArtifact::getStatus, RunArtifact::getDownStreamPolicy)
                .containsExactly(
                        org.assertj.core.api.Assertions.tuple(
                                "normalized",
                                RunArtifactStatusEnum.INTERMEDIATE,
                                DownStreamPolicyEnum.CONNECTABLE
                        ),
                        org.assertj.core.api.Assertions.tuple(
                                "report",
                                RunArtifactStatusEnum.PRIMARY,
                                DownStreamPolicyEnum.TERMINAL
                        )
                );
    }

    @Test
    void getExperimentArtifactsReturnsTransformedArtifact() {
        RunArtifactRepository runArtifactRepository = mock(RunArtifactRepository.class);
        RunArtifactService service = new RunArtifactService(
                runArtifactRepository,
                new ExperimentRunTransformer(),
                mock(ObjectStorageGateway.class)
        );
        ExperimentOpsHeaders headers = new ExperimentOpsHeaders();
        headers.setWorkspaceUuid("workspace-1");
        RunArtifact artifact = RunArtifact.builder()
                .experimentRunUuid("run-1")
                .workspaceUuid("workspace-1")
                .artifactType("report")
                .storageUri("s3://bucket/report.json")
                .format("JSON")
                .size(100L)
                .stepCount(2)
                .portName("report")
                .status(RunArtifactStatusEnum.PRIMARY)
                .downStreamPolicy(DownStreamPolicyEnum.TERMINAL)
                .build();
        artifact.setUuid("artifact-1");
        when(runArtifactRepository.findByExperimentRunUuidAndWorkspaceUuidAndDownStreamPolicyInAndEnabledOrderByStepCountAscPortNameAsc(
                "run-1",
                "workspace-1",
                List.of(DownStreamPolicyEnum.CONNECTABLE, DownStreamPolicyEnum.TERMINAL),
                true
        )).thenReturn(List.of(artifact));

        RunArtifactListResponseModel response = service.getExperimentArtifacts("run-1", headers);
        RunArtifactResponseModel responseArtifact = response.getData().getFirst();

        assertThat(response.getData()).hasSize(1);
        assertThat(responseArtifact.getUuid()).isEqualTo("artifact-1");
        assertThat(responseArtifact.getArtifactType()).isEqualTo("report");
        assertThat(responseArtifact.getFormat()).isEqualTo("JSON");
        assertThat(responseArtifact.getSize()).isEqualTo(100);
        assertThat(responseArtifact.getStepCount()).isEqualTo(2);
        assertThat(responseArtifact.getPortName()).isEqualTo("report");
        assertThat(responseArtifact.getStatus()).isEqualTo("PRIMARY");
        assertThat(responseArtifact.getDownstreamPolicy()).isEqualTo("TERMINAL");
    }

    @Test
    void getRunArtifactDownloadUrlReturnsPresignedReadUrl() {
        RunArtifactRepository runArtifactRepository = mock(RunArtifactRepository.class);
        ObjectStorageGateway objectStorageGateway = mock(ObjectStorageGateway.class);
        RunArtifactService service = new RunArtifactService(
                runArtifactRepository,
                new ExperimentRunTransformer(),
                objectStorageGateway
        );
        ExperimentOpsHeaders headers = new ExperimentOpsHeaders();
        headers.setWorkspaceUuid("workspace-1");
        RunArtifact artifact = RunArtifact.builder()
                .workspaceUuid("workspace-1")
                .storageUri("s3://bucket/report.json")
                .build();
        artifact.setUuid("artifact-1");
        Instant expiresAt = Instant.parse("2026-07-21T10:15:30Z");
        when(runArtifactRepository.findByUuidAndWorkspaceUuidAndEnabled("artifact-1", "workspace-1", true))
                .thenReturn(Optional.of(artifact));
        when(objectStorageGateway.createPresignedReadUrl("s3://bucket/report.json", headers))
                .thenReturn(new ObjectStorageGateway.PresignedObjectRead("https://example.test/report.json", expiresAt));

        RunArtifactDownloadUrlResponseModel response = service.getRunArtifactDownloadUrl("artifact-1", headers);

        assertThat(response.getDownloadUrl().toString()).isEqualTo("https://example.test/report.json");
        assertThat(response.getExpiresAt().toInstant()).isEqualTo(expiresAt);
    }

    private static ExperimentRunCompletedEvent completedEvent() {
        return new ExperimentRunCompletedEvent(
                ExperimentOpsMetadataEvent.newBuilder()
                        .setUuid("run-1")
                        .setWorkspaceUuid("workspace-1")
                        .setEventType("EXPERIMENT_RUN_COMPLETED")
                        .setRequesterUuid("user-1")
                        .setTraceUuid("request-1")
                        .setEventUuid("event-1")
                        .setEventTimestamp(1L)
                        .setRequestTimestamp(1L)
                        .build(),
                new ExperimentRunCompletedEventPayload(
                        null,
                        null,
                        null,
                        "SUCCEEDED",
                        new ExperimentRunCompletedResult(
                                List.of(
                                        new ExperimentRunCompletedArtifact(
                                                "CSV",
                                                "normalized",
                                                "s3://bucket/normalized.csv",
                                                100L,
                                                "CSV_PROFILE_ANALYSIS",
                                                1,
                                                "normalized"
                                        ),
                                        new ExperimentRunCompletedArtifact(
                                                "JSON",
                                                "report",
                                                "s3://bucket/report.json",
                                                100L,
                                                "CSV_PROFILE_ANALYSIS",
                                                2,
                                                "report"
                                        )
                                ),
                                List.of(),
                                null
                        ),
                        null
                )
        );
    }
}
