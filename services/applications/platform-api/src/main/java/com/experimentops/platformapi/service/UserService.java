package com.experimentops.platformapi.service;

import com.experimentops.avroevent.type.EventType;
import com.experimentops.common.exceptions.runtime.EntityAlreadyExistsException;
import com.experimentops.common.kafka.KafkaProducer;
import com.experimentops.notification.event.NotificationEvent;
import com.experimentops.platformapi.dal.gateway.KeyCloakGateway;
import com.experimentops.platformapi.dal.repository.UserRepository;
import com.experimentops.platformapi.model.entity.User;
import com.experimentops.platformapi.model.type.StatusEnum;
import com.experimentops.platformapi.transformer.UserTransformer;
import com.experimentops.platformapi.transformer.WorkspaceTransformer;
import com.experimentops.platformapi.util.EmailSubjectConstants;
import com.experimentops.platformapi.util.EmailTemplateUtil;
import com.experimentops.platformapi.validator.UserValidator;
import com.experimentops.user.event.UserMutationEvent;
import com.experimentops.user.model.v1.UserListResponseModel;
import com.experimentops.user.model.v1.UserRequestModel;
import com.experimentops.user.model.v1.UserResponseModel;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.ExperimentOpsUtils;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import com.experimentops.workspace.event.WorkspaceMutationEvent;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final UserTransformer userTransformer;
    private final KafkaProducer kafkaProducer;
    private final WorkspaceTransformer workspaceTransformer;
    private final UserValidator userValidator;
    private final KeyCloakGateway keyCloakGateway;
    private final WorkspaceQueryService workspaceQueryService;

    @Value("${workspace.topic.name}")
    private String workspaceCreationCompletedTopic;

    @Value("${user.topic.name}")
    private String userTopic;

    @Value("${notification.topic.name}")
    private String notificationTopic;

    @Value("${keycloak.password.policy.length}")
    private int userPasswordLength;

    @Value("${keycloak.password.policy.digits}")
    private int userPasswordDigitsLength;

    @Value("${keycloak.password.policy.specialChars}")
    private int userPasswordSpecialCharLength;

    public UserResponseModel publishUser(@NonNull UserRequestModel userRequestModel, @NonNull ExperimentOpsHeaders experimentOpsHeaders) {
        log.info(experimentOpsHeaders, "creating new user with email" + userRequestModel.getEmail());
        userValidator.validateUserRequestModel(userRequestModel);
        userRepository
                .findByEmailAndWorkspaceUuidAndStatusAndEnabled(
                        userRequestModel.getEmail(),
                        experimentOpsHeaders.getWorkspaceUuid(),
                        StatusEnum.ACTIVE,
                        true)
                .ifPresent(workspace -> {
                    throw new EntityAlreadyExistsException("email", userRequestModel.getEmail());
                });
        String tempPassword = ExperimentOpsUtils.generatePassword(
                userPasswordLength,
                userPasswordDigitsLength,
                userPasswordSpecialCharLength);

        String workspaceName = workspaceQueryService.getActiveWorkspaceNameByUuid(experimentOpsHeaders.getWorkspaceUuid()).getName();
        UserMutationEvent userMutationEvent = userTransformer.transformUserCreationEvent(userRequestModel, experimentOpsHeaders, tempPassword);
        keyCloakGateway.createWorkspaceUser(userMutationEvent, experimentOpsHeaders, userRequestModel.getUserRole(), workspaceName);
        NotificationEvent notificationEvent = userTransformer.transformNotificationEvent(
                userRequestModel.getEmail(),
                EmailSubjectConstants.USER_INVITE,
                EmailTemplateUtil.buildUserInviteBody(userRequestModel.getEmail(), tempPassword),
                EventType.USER_INVITE,
                experimentOpsHeaders
        );
        kafkaProducer.sendMessage(notificationTopic, notificationEvent, notificationEvent.getMetadata());
        kafkaProducer.sendMessage(userTopic, userMutationEvent, userMutationEvent.getMetadata());
        return userTransformer.transformUserResponseModel(userMutationEvent, experimentOpsHeaders);
    }

    public void createUser(UserMutationEvent userMutationEvent, ExperimentOpsHeaders headers){
        // Adding Idempotency check
        String userUuid = userMutationEvent.getMetadata().getUuid();
        userRepository
                .findByUuidAndWorkspaceUuidAndStatusAndEnabled(userUuid, headers.getWorkspaceUuid(), StatusEnum.ACTIVE, true)
                .ifPresent(user ->
                    log.error(headers,"user create event is already consumed")
                );

        User user = userTransformer.transformUserEntity(userMutationEvent, headers);
        userRepository.save(user);

        if (userMutationEvent.getPayload().getWorkspaceCreation()){
            WorkspaceMutationEvent workspaceMutationEvent = workspaceTransformer.transformWorkspaceCreationCompletionEvent(headers);
            kafkaProducer.sendMessage(workspaceCreationCompletedTopic, workspaceMutationEvent, workspaceMutationEvent.getMetadata());
        }
    }

    @NonNull
    public UserListResponseModel getUserList(Integer page, Integer size, @NonNull ExperimentOpsHeaders headers) {
        log.info(headers, "getting user list for workspace uuid " + headers.getWorkspaceUuid());
        Pageable pageable = PaginationUtil.createPageRequest(page, size);
        Page<User> usersPage = userRepository
                .findAllByWorkspaceUuidAndStatusAndEnabledOrderByCreationDateDesc(headers.getWorkspaceUuid(), StatusEnum.ACTIVE, true, pageable);
        List<UserResponseModel> users = usersPage
                .getContent()
                .stream()
                .map(userTransformer::transformUserResponseModel)
                .toList();
        return userTransformer.transformUserListResponseModel(users, usersPage.getTotalElements());
    }
}
