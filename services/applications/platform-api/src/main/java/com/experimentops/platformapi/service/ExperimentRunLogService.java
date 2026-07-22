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
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class ExperimentRunLogService {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(ExperimentRunLogService.class);

    private final ExperimentRunRepository experimentRunRepository;
    private final ExperimentRunLogRepository experimentRunLogRepository;
    private final ExperimentRunTransformer experimentRunTransformer;
    private final ObjectStorageGateway objectStorageGateway;

    public ExperimentRunLogListResponseModel getExperimentRunLogs(
            @NonNull String experimentRunUuid,
            @Nullable Integer page,
            @Nullable Integer size,
            @NonNull ExperimentOpsHeaders headers) {

        log.info(headers, "getting experiment run logs for experiment run uuid " + experimentRunUuid);
        getExperimentRunEntity(experimentRunUuid, headers);
        Pageable pageable = PaginationUtil.createPageRequest(page, size);
        Page<ExperimentRunLog> experimentRunLogPage = experimentRunLogRepository
                .findByExperimentRunUuidAndEnabledOrderByTimestampAsc(
                        experimentRunUuid,
                        true,
                        pageable
                );
        return experimentRunTransformer.transformExperimentRunLogListResponseModel(experimentRunLogPage);
    }

    @NonNull
    public ExperimentRunLogDownloadUrlResponseModel getExperimentRunLogDownloadUrl(
            @NonNull String experimentRunUuid,
            @NonNull ExperimentOpsHeaders headers) {

        log.info(headers, "creating download url for experiment run logs uuid " + experimentRunUuid);
        ExperimentRun experimentRun = getExperimentRunEntity(experimentRunUuid, headers);

        if (StringUtils.isBlank(experimentRun.getLogs())) {
            throw new EntityNotFoundException("experiment run logs", experimentRunUuid);
        }

        ObjectStorageGateway.PresignedObjectRead readUrl = objectStorageGateway.createPresignedReadUrl(experimentRun.getLogs(), headers);
        return new ExperimentRunLogDownloadUrlResponseModel()
                .downloadUrl(URI.create(readUrl.downloadUrl()))
                .expiresAt(OffsetDateTime.ofInstant(readUrl.expiresAt(), ZoneOffset.UTC));
    }

    @NonNull
    private ExperimentRun getExperimentRunEntity(
            @NonNull String experimentRunUuid,
            @NonNull ExperimentOpsHeaders headers) {

        return experimentRunRepository
                .findByUuidAndWorkspaceUuidAndEnabled(experimentRunUuid, headers.getWorkspaceUuid(), true)
                .orElseThrow(() -> new EntityNotFoundException("experiment run uuid", experimentRunUuid));
    }
}
