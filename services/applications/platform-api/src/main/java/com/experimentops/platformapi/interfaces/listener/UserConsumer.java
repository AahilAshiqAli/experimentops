package com.experimentops.platformapi.interfaces.listener;

import com.experimentops.avroevent.type.EventType;
import com.experimentops.common.kafka.utils.ExperimentOpsMetadataUtil;
import com.experimentops.platformapi.service.UserService;
import com.experimentops.user.event.UserMutationEvent;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserConsumer {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(UserConsumer.class);
    public static final String CONSUMING_MESSAGE = "Consuming message: ";

    private final UserService userService;

    @KafkaListener(topics = "${user.topic.name}", groupId = "${user.topic.group-id}")
    public void consume(ConsumerRecord<String, UserMutationEvent> userRecord) {
        UserMutationEvent event = userRecord.value();
        if (EventType.USER_CREATE == EventType.valueOf(event.getMetadata().getEventType())) {
            ExperimentOpsHeaders headers = ExperimentOpsMetadataUtil.extractHeaders(event.getMetadata());
            log.info(headers, CONSUMING_MESSAGE + event);
            userService.createUser(event, headers);
        }
    }
}
