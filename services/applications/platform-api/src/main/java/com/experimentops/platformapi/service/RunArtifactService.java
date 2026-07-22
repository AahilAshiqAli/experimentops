package com.experimentops.platformapi.service;

import com.experimentops.common.exceptions.runtime.EntityNotFoundException;
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
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RunArtifactService {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(RunArtifactService.class);

    private final RunArtifactRepository runArtifactRepository;
    private final ExperimentRunTransformer experimentRunTransformer;
    private final ObjectStorageGateway objectStorageGateway;

    public void uploadArtifacts(@NonNull ExperimentRunCompletedEvent event, @NonNull String experimentRunUuid, @NonNull ExperimentRunResolvedPlan resolvedPlan, @NonNull ExperimentOpsHeaders headers) {
        ExperimentRunCompletedEventPayload payload = event.getPayload();

        ExperimentRunCompletedResult result = payload.getResult();
        List<ExperimentRunCompletedArtifact> artifacts = result.getArtifact();
        Map<ArtifactReference, ExperimentRunResolvedPlan.Output> outputsByReference = outputsByReference(resolvedPlan);
        Integer lastStepCount = lastStepCount(resolvedPlan);

        log.info(headers, "saving run artifact objects");
        List<RunArtifact> runArtifacts = artifacts.stream()
                .map(artifact -> {
                    DownStreamPolicyEnum downStreamPolicy = outputsByReference
                            .get(new ArtifactReference(artifact.getStepCount(), artifact.getPortName()))
                            .getDownStreamPolicy();
                    return experimentRunTransformer.transformRunArtifactEntity(
                            experimentRunUuid,
                            artifact,
                            toArtifactStatus(artifact.getStepCount(), lastStepCount),
                            downStreamPolicy,
                            headers
                    );
                })
                .toList();
        runArtifactRepository.saveAll(runArtifacts);
    }

    @NonNull
    public RunArtifactListResponseModel getExperimentArtifacts(@NonNull String experimentRunUuid, @NonNull ExperimentOpsHeaders headers) {
        log.info(headers, "fetch run artifact objects for experiment run " + experimentRunUuid);
        List<RunArtifact> runArtifacts = runArtifactRepository.findByExperimentRunUuidAndWorkspaceUuidAndDownStreamPolicyInAndEnabledOrderByStepCountAscPortNameAsc(
                experimentRunUuid,
                headers.getWorkspaceUuid(),
                List.of(DownStreamPolicyEnum.CONNECTABLE, DownStreamPolicyEnum.TERMINAL),
                true
        );
        List<RunArtifactResponseModel> runArtifactResponseModels = runArtifacts.stream()
                .map(runArtifact -> experimentRunTransformer.transformRunArtifactListResponseModel(runArtifact, headers))
                .toList();

        return new RunArtifactListResponseModel().data(runArtifactResponseModels);
    }

    @NonNull
    public RunArtifactDownloadUrlResponseModel getRunArtifactDownloadUrl(@NonNull String artifactUuid, @NonNull ExperimentOpsHeaders headers) {
        log.info(headers, "creating download url for run artifact uuid " + artifactUuid);
        RunArtifact runArtifact = runArtifactRepository
                .findByUuidAndWorkspaceUuidAndEnabled(artifactUuid, headers.getWorkspaceUuid(), true)
                .orElseThrow(() -> new EntityNotFoundException("run artifact uuid", artifactUuid));
        ObjectStorageGateway.PresignedObjectRead readUrl = objectStorageGateway.createPresignedReadUrl(runArtifact.getStorageUri(), headers);
        return new RunArtifactDownloadUrlResponseModel()
                .downloadUrl(URI.create(readUrl.downloadUrl()))
                .expiresAt(OffsetDateTime.ofInstant(readUrl.expiresAt(), ZoneOffset.UTC));
    }

    @NonNull
    private Map<ArtifactReference, ExperimentRunResolvedPlan.Output> outputsByReference(@NonNull ExperimentRunResolvedPlan resolvedPlan) {

        return resolvedPlan.getSteps()
                .stream()
                .flatMap(step -> step.getOutputs()
                        .stream()
                        .map(output -> Map.entry(new ArtifactReference(step.getStepCount(), output.getName()), output)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @NonNull
    private Integer lastStepCount(@NonNull ExperimentRunResolvedPlan resolvedPlan) {
        return resolvedPlan.getSteps()
                .stream()
                .map(ExperimentRunResolvedPlan.Step::getStepCount)
                .max(Integer::compareTo)
                .orElse(0);
    }

    @NonNull
    private RunArtifactStatusEnum toArtifactStatus(Integer artifactStepCount, Integer lastStepCount) {
        return lastStepCount.equals(artifactStepCount)
                ? RunArtifactStatusEnum.PRIMARY
                : RunArtifactStatusEnum.INTERMEDIATE;
    }

    private record ArtifactReference(Integer stepCount, String portName) {
    }

}
