package com.experimentops.platformapi.service;

import com.experimentops.common.exceptions.runtime.EntityAlreadyExistsException;
import com.experimentops.common.exceptions.runtime.EntityNotFoundException;
import com.experimentops.common.kafka.KafkaProducer;
import com.experimentops.platformapi.dal.gateway.KeyCloakGateway;
import com.experimentops.platformapi.dal.repository.WorkspaceRepository;
import com.experimentops.platformapi.model.entity.Workspace;
import com.experimentops.platformapi.model.type.StatusEnum;
import com.experimentops.platformapi.transformer.UserTransformer;
import com.experimentops.platformapi.transformer.WorkspaceTransformer;
import com.experimentops.platformapi.validator.WorkspaceValidator;
import com.experimentops.user.event.UserMutationEvent;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import com.experimentops.workspace.event.WorkspaceMutationEvent;
import com.experimentops.workspace.model.v1.WorkspaceRequestModel;
import com.experimentops.workspace.model.v1.WorkspaceResponseModel;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class WorkspaceService {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(WorkspaceService.class);

    private final WorkspaceValidator workspaceValidator;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceTransformer workspaceTransformer;
    private final KafkaProducer kafkaProducer;
    private final KeyCloakGateway keyCloakGateway;
    private final UserTransformer userTransformer;

    @Value("${workspace.topic.name}")
    private String workspaceTopic;

    @Value("${user.topic.name}")
    private String userTopic;

    @NonNull
    public WorkspaceResponseModel publishWorkspaceCreationEvent(@NonNull WorkspaceRequestModel workspaceRequestModel, @NonNull ExperimentOpsHeaders headers) {
        log.info(headers, "creating new workspace with name " + workspaceRequestModel.getWorkspaceName());
        workspaceValidator.validateWorkspaceRequestModel(workspaceRequestModel);
        workspaceRepository
                .findByNameOrEmailAndStatusAndEnabled(
                        workspaceRequestModel.getWorkspaceName(),
                        workspaceRequestModel.getWorkspaceEmail(),
                        StatusEnum.ACTIVE,
                        true)
                .ifPresent(workspace -> {
                            throw new EntityAlreadyExistsException("name or email", workspaceRequestModel.getWorkspaceName() + workspaceRequestModel.getWorkspaceEmail());
                        });
        WorkspaceMutationEvent workspaceMutationEvent = workspaceTransformer.transformWorkspaceCreationEvent(workspaceRequestModel, headers);
        keyCloakGateway.createWorkspaceRealm(workspaceMutationEvent, headers);
        kafkaProducer.sendMessage(workspaceTopic, workspaceMutationEvent, workspaceMutationEvent.getMetadata());
        return workspaceTransformer.transformWorkspaceResponseModel(workspaceMutationEvent, headers);
    }

    public void createWorkspace(@NonNull WorkspaceMutationEvent workspaceMutationEvent, @NonNull ExperimentOpsHeaders headers){
        Workspace workspace = workspaceTransformer.transformWorkspaceEntity(workspaceMutationEvent, headers);
        workspaceRepository.save(workspace);
        log.info(headers, "saved workspace with workspace uuid: " + workspace.getUuid());
        UserMutationEvent userMutationEvent = userTransformer.transformUserCreationEvent(workspaceMutationEvent, headers);
        kafkaProducer.sendMessage(userTopic, userMutationEvent, userMutationEvent.getMetadata());
    }

    public void changeStatusWorkspace(@NonNull ExperimentOpsHeaders headers){
        String workspaceUuid = headers.getWorkspaceUuid();
        workspaceRepository
                .findByUuidAndStatusAndEnabled(workspaceUuid, StatusEnum.PENDING, true)
                .ifPresentOrElse(
                        workspace -> {
                            workspace.setStatus(StatusEnum.ACTIVE);
                            workspaceRepository.save(workspace);
                        },
                        () -> {
                            throw new EntityNotFoundException("Workspace uuid", workspaceUuid);
                        }
                );
        log.info(headers, "workspace creation completed");
    }
}
