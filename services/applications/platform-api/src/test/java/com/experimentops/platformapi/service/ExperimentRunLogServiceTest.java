package com.experimentops.platformapi.service;

import com.experimentops.common.exceptions.runtime.EntityNotFoundException;
import com.experimentops.experiment.run.model.v1.ExperimentRunLogDownloadUrlResponseModel;
import com.experimentops.experiment.run.model.v1.ExperimentRunLogListResponseModel;
import com.experimentops.objectstorage.gateway.ObjectStorageGateway;
import com.experimentops.platformapi.dal.repository.ExperimentRunLogRepository;
import com.experimentops.platformapi.dal.repository.ExperimentRunRepository;
import com.experimentops.platformapi.model.entity.ExperimentRun;
import com.experimentops.platformapi.model.entity.ExperimentRunLog;
import com.experimentops.platformapi.transformer.ExperimentRunTransformer;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExperimentRunLogServiceTest {

    @Test
    void getExperimentRunLogDownloadUrlReturnsPresignedReadUrl() {
        ExperimentRunRepository experimentRunRepository = mock(ExperimentRunRepository.class);
        ObjectStorageGateway objectStorageGateway = mock(ObjectStorageGateway.class);
        ExperimentRunLogService service = service(
                experimentRunRepository,
                mock(ExperimentRunLogRepository.class),
                objectStorageGateway
        );
        ExperimentOpsHeaders headers = headers();
        ExperimentRun experimentRun = ExperimentRun.builder()
                .workspaceUuid("workspace-1")
                .logs("s3://bucket/run.log")
                .build();
        experimentRun.setUuid("run-1");
        Instant expiresAt = Instant.parse("2026-07-21T10:15:30Z");

        when(experimentRunRepository.findByUuidAndWorkspaceUuidAndEnabled("run-1", "workspace-1", true))
                .thenReturn(Optional.of(experimentRun));
        when(objectStorageGateway.createPresignedReadUrl("s3://bucket/run.log", headers))
                .thenReturn(new ObjectStorageGateway.PresignedObjectRead("https://example.test/run.log", expiresAt));

        ExperimentRunLogDownloadUrlResponseModel response = service.getExperimentRunLogDownloadUrl("run-1", headers);

        assertThat(response.getDownloadUrl().toString()).isEqualTo("https://example.test/run.log");
        assertThat(response.getExpiresAt().toInstant()).isEqualTo(expiresAt);
    }

    @Test
    void getExperimentRunLogDownloadUrlThrowsWhenRunHasNoLogStorageUri() {
        ExperimentRunRepository experimentRunRepository = mock(ExperimentRunRepository.class);
        ExperimentRunLogService service = service(
                experimentRunRepository,
                mock(ExperimentRunLogRepository.class),
                mock(ObjectStorageGateway.class)
        );
        ExperimentOpsHeaders headers = headers();
        ExperimentRun experimentRun = ExperimentRun.builder()
                .workspaceUuid("workspace-1")
                .logs(" ")
                .build();
        experimentRun.setUuid("run-1");

        when(experimentRunRepository.findByUuidAndWorkspaceUuidAndEnabled("run-1", "workspace-1", true))
                .thenReturn(Optional.of(experimentRun));

        assertThatThrownBy(() -> service.getExperimentRunLogDownloadUrl("run-1", headers))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getExperimentRunLogsReturnsPagedLogsOrderedByRepositoryTimestampOrder() {
        ExperimentRunRepository experimentRunRepository = mock(ExperimentRunRepository.class);
        ExperimentRunLogRepository experimentRunLogRepository = mock(ExperimentRunLogRepository.class);
        ExperimentRunLogService service = service(
                experimentRunRepository,
                experimentRunLogRepository,
                mock(ObjectStorageGateway.class)
        );
        ExperimentOpsHeaders headers = headers();
        ExperimentRun experimentRun = ExperimentRun.builder()
                .workspaceUuid("workspace-1")
                .build();
        experimentRun.setUuid("run-1");
        ExperimentRunLog firstLog = ExperimentRunLog.builder()
                .experimentRunUuid("run-1")
                .sequence(10L)
                .timestamp(Timestamp.valueOf("2026-01-01 00:00:01"))
                .level("INFO")
                .experimentType("CSV_PROFILE_ANALYSIS")
                .message("started")
                .build();
        ExperimentRunLog secondLog = ExperimentRunLog.builder()
                .experimentRunUuid("run-1")
                .sequence(12L)
                .timestamp(Timestamp.valueOf("2026-01-01 00:00:02"))
                .level("WARN")
                .experimentType("CSV_PROFILE_ANALYSIS")
                .message("still running")
                .build();
        PageRequest pageRequest = PageRequest.of(0, 2);

        when(experimentRunRepository.findByUuidAndWorkspaceUuidAndEnabled("run-1", "workspace-1", true))
                .thenReturn(Optional.of(experimentRun));
        when(experimentRunLogRepository.findByExperimentRunUuidAndEnabledOrderByTimestampAsc(
                "run-1",
                true,
                pageRequest
        )).thenReturn(new PageImpl<>(List.of(firstLog, secondLog), pageRequest, 5));

        ExperimentRunLogListResponseModel response = service.getExperimentRunLogs(
                "run-1",
                0,
                2,
                headers
        );

        assertThat(response.getTotalElements()).isEqualTo(5);
        assertThat(response.getData()).hasSize(2);
        assertThat(response.getData().get(0).getSequence()).isEqualTo(10L);
        assertThat(response.getData().get(0).getMessage()).isEqualTo("started");
        assertThat(response.getData().get(1).getSequence()).isEqualTo(12L);
        assertThat(response.getData().get(1).getMessage()).isEqualTo("still running");
        verify(experimentRunLogRepository).findByExperimentRunUuidAndEnabledOrderByTimestampAsc(
                "run-1",
                true,
                pageRequest
        );
    }

    private static ExperimentRunLogService service(
            ExperimentRunRepository experimentRunRepository,
            ExperimentRunLogRepository experimentRunLogRepository,
            ObjectStorageGateway objectStorageGateway) {

        return new ExperimentRunLogService(
                experimentRunRepository,
                experimentRunLogRepository,
                new ExperimentRunTransformer(),
                objectStorageGateway
        );
    }

    private static ExperimentOpsHeaders headers() {
        ExperimentOpsHeaders headers = new ExperimentOpsHeaders();
        headers.setWorkspaceUuid("workspace-1");
        headers.setUserUuid("user-1");
        return headers;
    }
}
