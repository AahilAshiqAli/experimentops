package com.experimentops.platformapi.service;

import com.experimentops.experiment.run.event.ExperimentRunCompletedArtifact;
import com.experimentops.experiment.run.event.ExperimentRunCompletedEvent;
import com.experimentops.experiment.run.event.ExperimentRunCompletedEventPayload;
import com.experimentops.experiment.run.event.ExperimentRunCompletedResult;
import com.experimentops.platformapi.dal.repository.RunArtifactRepository;
import com.experimentops.platformapi.model.entity.RunArtifact;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.ExperimentOpsUtils;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RunArtifactService {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(RunArtifactService.class);

    private final RunArtifactRepository runArtifactRepository;

    public void uploadArtifacts(@NonNull ExperimentRunCompletedEvent event, @NonNull ExperimentOpsHeaders headers) {
        ExperimentRunCompletedEventPayload payload = event.getPayload();

        ExperimentRunCompletedResult result = payload.getResult();
        List<ExperimentRunCompletedArtifact> artifacts = result.getArtifact();

        log.info(headers, "saving run artifact objects");
        artifacts.forEach(artifact -> runArtifactRepository.save(transformRunArtifactEntity(event, artifact, headers)));
    }

    @NonNull
    private RunArtifact transformRunArtifactEntity(
            @NonNull ExperimentRunCompletedEvent event,
            @NonNull ExperimentRunCompletedArtifact artifact,
            @NonNull ExperimentOpsHeaders headers) {

        RunArtifact runArtifact = RunArtifact.builder()
                .experimentRunUuid(event.getMetadata().getUuid())
                .workspaceUuid(headers.getWorkspaceUuid())
                .artifactType(artifact.getType())
                .storageUri(artifact.getUri())
                .format(artifact.getFormat())
                .size(artifact.getSize())
                .build();
        runArtifact.setUuid(ExperimentOpsUtils.uuid());

        return runArtifact;
    }

}
