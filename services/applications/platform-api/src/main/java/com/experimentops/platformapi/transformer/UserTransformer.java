package com.experimentops.platformapi.transformer;

import com.experimentops.avroevent.type.EventType;
import com.experimentops.common.kafka.model.event.ExperimentOpsMetadataEvent;
import com.experimentops.common.kafka.utils.ExperimentOpsMetadataUtil;
import com.experimentops.platformapi.model.entity.User;
import com.experimentops.platformapi.model.type.StatusEnum;
import com.experimentops.user.event.UserMutationEvent;
import com.experimentops.user.event.UserMutationEventPayload;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.ExperimentOpsUtils;
import com.experimentops.utils.constant.RoleType;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import com.experimentops.workspace.event.WorkspaceMutationEvent;
import com.experimentops.workspace.event.WorkspaceMutationEventPayload;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

@Component
public class UserTransformer {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(UserTransformer.class);

    public UserMutationEvent transformUserCreationEvent(@NonNull WorkspaceMutationEvent workspaceMutationEvent, @NonNull ExperimentOpsHeaders headers) {

        log.info(headers, "transforming the payload to user mutation event");
        WorkspaceMutationEventPayload workspacePayload = workspaceMutationEvent.getPayload();

        UserMutationEventPayload payload = UserMutationEventPayload.newBuilder()
                .setAdminEmail(workspacePayload.getAdminEmail())
                .setAdminFirstName(workspacePayload.getAdminFirstName())
                .setAdminLastName(workspacePayload.getAdminLastName())
                .setAdminPassword(workspacePayload.getAdminPassword())
                .setUserRole(RoleType.WORKSPACE_ADMIN.getRoleName())
                .build();

        headers.setWorkspaceUuid(workspaceMutationEvent.getMetadata().getUuid());

        ExperimentOpsMetadataEvent metadata = ExperimentOpsMetadataUtil.metadataEvent(
                headers,
                ExperimentOpsUtils.uuid(),
                EventType.USER_CREATE.name(),
                this.getClass().getSimpleName()
        );

        return UserMutationEvent.newBuilder()
                .setMetadata(metadata)
                .setPayload(payload)
                .build();
    }

    public User transformUserEntity(@NonNull UserMutationEvent userMutationEvent, @NonNull ExperimentOpsHeaders headers){
        log.info(headers, "transforming the payload to user entity");

        UserMutationEventPayload payload = userMutationEvent.getPayload();
        User user = User.builder()
                .email(payload.getAdminEmail())
                .firstName(payload.getAdminFirstName())
                .lastName(payload.getAdminLastName())
                .status(StatusEnum.ACTIVE)
                .role(payload.getUserRole())
                .build();

        user.setUuid(userMutationEvent.getMetadata().getUuid());
        return user;
    }
}
