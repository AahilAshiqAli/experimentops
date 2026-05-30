package com.experimentops.platformapi.service;

import com.experimentops.common.exceptions.runtime.EntityAlreadyExistsException;
import com.experimentops.common.kafka.KafkaProducer;
import com.experimentops.platformapi.dal.repository.UserRepository;
import com.experimentops.platformapi.model.entity.User;
import com.experimentops.platformapi.model.type.StatusEnum;
import com.experimentops.platformapi.transformer.UserTransformer;
import com.experimentops.platformapi.transformer.WorkspaceTransformer;
import com.experimentops.user.event.UserMutationEvent;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import com.experimentops.workspace.event.WorkspaceMutationEvent;
import lombok.RequiredArgsConstructor;
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

    @Value("${workspace.topic.name}")
    private String workspaceCreationCompletedTopic;

    public void createUser(UserMutationEvent userMutationEvent, ExperimentOpsHeaders headers){
        // Adding Idempotency check
        String userUuid = userMutationEvent.getMetadata().getUuid();
        userRepository
                .findByUuidAndStatusAndEnabled(userUuid, StatusEnum.ACTIVE, true)
                .ifPresent(workspace -> {
                    throw new EntityAlreadyExistsException("uuid", userUuid);
                });

        User user = userTransformer.transformUserEntity(userMutationEvent, headers);
        userRepository.save(user);
        WorkspaceMutationEvent workspaceMutationEvent = workspaceTransformer.transformWorkspaceCreationCompletionEvent(headers);
        kafkaProducer.sendMessage(workspaceCreationCompletedTopic, userMutationEvent, userMutationEvent.getMetadata());
    }




}
