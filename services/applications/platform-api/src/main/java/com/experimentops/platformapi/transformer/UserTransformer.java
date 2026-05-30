package com.experimentops.platformapi.transformer;

import com.experimentops.avroevent.type.EventType;
import com.experimentops.common.kafka.model.event.ExperimentOpsMetadataEvent;
import com.experimentops.common.kafka.utils.ExperimentOpsMetadataUtil;
import com.experimentops.platformapi.model.entity.User;
import com.experimentops.platformapi.model.type.StatusEnum;
import com.experimentops.user.event.UserMutationEvent;
import com.experimentops.user.event.UserMutationEventPayload;
import com.experimentops.user.model.v1.UserRequestModel;
import com.experimentops.user.model.v1.UserResponseModel;
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
                .setUserEmail(workspacePayload.getAdminEmail())
                .setUserFirstName(workspacePayload.getAdminFirstName())
                .setUserLastName(workspacePayload.getAdminLastName())
                .setUserPassword(workspacePayload.getAdminPassword())
                .setUserRole(RoleType.WORKSPACE_ADMIN.getRoleName())
                .build();

        headers.setWorkspaceUuid(workspaceMutationEvent.getMetadata().getUuid());

        ExperimentOpsMetadataEvent metadata = ExperimentOpsMetadataUtil.metadataEvent(
                headers,
                workspacePayload.getUserUuid(),
                EventType.USER_CREATE.name(),
                this.getClass().getSimpleName()
        );

        return UserMutationEvent.newBuilder()
                .setMetadata(metadata)
                .setPayload(payload)
                .build();
    }

    public UserMutationEvent transformUserCreationEvent(@NonNull UserRequestModel userRequestModel, @NonNull ExperimentOpsHeaders headers) {

        log.info(headers, "transforming the payload to user mutation event");

        UserMutationEventPayload payload = UserMutationEventPayload.newBuilder()
                .setUserEmail(userRequestModel.getEmail())
                .setUserFirstName(userRequestModel.getFirstName())
                .setUserLastName(userRequestModel.getLastName())
                .setUserPassword(userRequestModel.getPassword())
                .setUserRole(userRequestModel.getUserRole())
                .build();

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

    public UserResponseModel transformUserResponseModel(@NonNull UserMutationEvent userMutationEvent, @NonNull ExperimentOpsHeaders headers) {

        log.info(headers, "transforming the payload to User Response Model");

        UserMutationEventPayload payload = userMutationEvent.getPayload();

        UserResponseModel userResponseModel = new UserResponseModel();
        userResponseModel.setUuid(userMutationEvent.getMetadata().getUuid());
        userResponseModel.setEmail(payload.getUserEmail());
        userResponseModel.setFirstName(payload.getUserFirstName());
        userResponseModel.setLastName(payload.getUserLastName());
        userResponseModel.setUserRole(payload.getUserRole());

        return userResponseModel;
    }

    public User transformUserEntity(@NonNull UserMutationEvent userMutationEvent, @NonNull ExperimentOpsHeaders headers){
        log.info(headers, "transforming the payload to user entity");

        UserMutationEventPayload payload = userMutationEvent.getPayload();
        User user = User.builder()
                .email(payload.getUserEmail())
                .firstName(payload.getUserFirstName())
                .lastName(payload.getUserLastName())
                .status(StatusEnum.ACTIVE)
                .role(payload.getUserRole())
                .workspaceUuid(headers.getWorkspaceUuid())
                .build();

        user.setUuid(userMutationEvent.getMetadata().getUuid());
        return user;
    }
}
