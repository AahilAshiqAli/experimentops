package com.experimentops.platformapi.transformer;

import com.experimentops.avroevent.type.EventType;
import com.experimentops.common.kafka.model.event.ExperimentOpsMetadataEvent;
import com.experimentops.common.kafka.utils.ExperimentOpsMetadataUtil;
import com.experimentops.platformapi.model.entity.Workspace;
import com.experimentops.platformapi.model.type.StatusEnum;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.ExperimentOpsUtils;
import com.experimentops.utils.constant.RoleType;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import com.experimentops.workspace.event.WorkspaceMutationEvent;
import com.experimentops.workspace.event.WorkspaceMutationEventPayload;
import com.experimentops.workspace.model.v1.WorkspaceRequestModel;
import com.experimentops.workspace.model.v1.WorkspaceResponseModel;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

@Component
public class WorkspaceTransformer {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(WorkspaceTransformer.class);

    public WorkspaceMutationEvent transformWorkspaceCreationEvent(@NonNull WorkspaceRequestModel workspaceRequestModel, @NonNull ExperimentOpsHeaders headers){

        log.info(headers, "transforming the payload to workspace mutation event");

        WorkspaceMutationEventPayload payload = WorkspaceMutationEventPayload.newBuilder()
                .setWorkspaceName(workspaceRequestModel.getWorkspaceName())
                .setWorkspaceEmail(workspaceRequestModel.getWorkspaceEmail())
                .setAdminEmail(workspaceRequestModel.getAdminUser().getEmail())
                .setAdminFirstName(workspaceRequestModel.getAdminUser().getFirstName())
                .setAdminLastName(workspaceRequestModel.getAdminUser().getLastName())
                .setAdminPassword(workspaceRequestModel.getAdminUser().getPassword())
                .build();

        ExperimentOpsMetadataEvent metadata = ExperimentOpsMetadataUtil.metadataEvent(
                headers,
                ExperimentOpsUtils.uuid(),
                EventType.WORKSPACE_CREATE.name(),
                this.getClass().getSimpleName()
        );

        return WorkspaceMutationEvent.newBuilder()
                .setMetadata(metadata)
                .setPayload(payload)
                .build();
    }

    public WorkspaceResponseModel transformWorkspaceResponseModel(@NonNull WorkspaceMutationEvent workspaceMutationEvent, @NonNull ExperimentOpsHeaders headers){

        log.info(headers, "transforming the payload to Workspace Response Model");

        WorkspaceMutationEventPayload payload = workspaceMutationEvent.getPayload();

        WorkspaceResponseModel workspaceResponseModel = new WorkspaceResponseModel();
        workspaceResponseModel.setWorkspaceUuid(workspaceMutationEvent.getMetadata().getUuid());
        workspaceResponseModel.setWorkspaceEmail(payload.getWorkspaceEmail());
        workspaceResponseModel.setWorkspaceName(payload.getWorkspaceName());
        workspaceResponseModel.setWorkspaceStatus(StatusEnum.PENDING.name());
        workspaceResponseModel.setUserRole(RoleType.WORKSPACE_ADMIN.getRoleName());
        workspaceResponseModel.setUserUuid(payload.getUserUuid());

        return workspaceResponseModel;
    }

    @NonNull
    public Workspace transformWorkspaceEntity(@NonNull WorkspaceMutationEvent workspaceMutationEvent, @NonNull ExperimentOpsHeaders headers){

        log.info(headers, "transforming the payload to Workspace Entity");

        WorkspaceMutationEventPayload payload = workspaceMutationEvent.getPayload();
        Workspace workspace = Workspace.builder()
                .userUuid(payload.getUserUuid())
                .email(payload.getWorkspaceEmail())
                .name(payload.getWorkspaceName())
                .status(StatusEnum.PENDING)
                .build();
        workspace.setUuid(workspaceMutationEvent.getMetadata().getUuid());

        return workspace;
    }

    @NonNull
    public WorkspaceMutationEvent transformWorkspaceCreationCompletionEvent(@NonNull ExperimentOpsHeaders headers){
        log.info(headers, "transforming the payload to workspace mutation event for workspace creation completion");
        ExperimentOpsMetadataEvent metadata = ExperimentOpsMetadataUtil.metadataEvent(
                headers,
                headers.getWorkspaceUuid(),
                EventType.WORKSPACE_CREATION_COMPLETE.name(),
                this.getClass().getSimpleName()
        );

        return WorkspaceMutationEvent.newBuilder()
                .setMetadata(metadata)
                .setPayload(null)
                .build();

    }

}
