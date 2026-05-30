package com.experimentops.platformapi.service;

import com.experimentops.common.exceptions.runtime.EntityAlreadyExistsException;
import com.experimentops.common.kafka.KafkaProducer;
import com.experimentops.platformapi.dal.repository.UserRepository;
import com.experimentops.platformapi.model.entity.User;
import com.experimentops.platformapi.model.type.StatusEnum;
import com.experimentops.platformapi.transformer.UserTransformer;
import com.experimentops.platformapi.transformer.WorkspaceTransformer;
import com.experimentops.platformapi.validator.UserValidator;
import com.experimentops.user.event.UserMutationEvent;
import com.experimentops.user.model.v1.UserRequestModel;
import com.experimentops.user.model.v1.UserResponseModel;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import com.experimentops.workspace.event.WorkspaceMutationEvent;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final UserTransformer userTransformer;
    private final KafkaProducer kafkaProducer;
    private final WorkspaceTransformer workspaceTransformer;
    private final UserValidator userValidator;

    @Value("${workspace.topic.name}")
    private String workspaceCreationCompletedTopic;

    @Value("${user.topic.name}")
    private String userTopic;

    public UserResponseModel publishUser(@NonNull ExperimentOpsHeaders experimentOpsHeaders, @NonNull UserRequestModel userRequestModel, boolean b) {
        log.info(experimentOpsHeaders, "creating new user with email" + userRequestModel.getEmail());
        userValidator.validateUserRequestModel(userRequestModel);
        UserMutationEvent userMutationEvent = userTransformer.transformUserCreationEvent(userRequestModel, experimentOpsHeaders);
        kafkaProducer.sendMessage(userTopic, userMutationEvent, userMutationEvent.getMetadata());
        return userTransformer.transformUserResponseModel(userMutationEvent, experimentOpsHeaders);
    }

    public void createUser(UserMutationEvent userMutationEvent, ExperimentOpsHeaders headers){
        // Adding Idempotency check
        String userUuid = userMutationEvent.getMetadata().getUuid();
        userRepository
                .findByUuidAndStatusAndEnabled(userUuid, StatusEnum.ACTIVE, true)
                .ifPresent(user -> {
                    throw new EntityAlreadyExistsException("uuid", userUuid);
                });

        User user = userTransformer.transformUserEntity(userMutationEvent, headers);
        userRepository.save(user);
        WorkspaceMutationEvent workspaceMutationEvent = workspaceTransformer.transformWorkspaceCreationCompletionEvent(headers);
        kafkaProducer.sendMessage(workspaceCreationCompletedTopic, workspaceMutationEvent, workspaceMutationEvent.getMetadata());
    }
}
