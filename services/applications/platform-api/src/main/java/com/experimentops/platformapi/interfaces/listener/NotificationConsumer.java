package com.experimentops.platformapi.interfaces.listener;

import com.experimentops.common.kafka.utils.ExperimentOpsMetadataUtil;
import com.experimentops.notification.event.NotificationEvent;
import com.experimentops.platformapi.service.NotificationService;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationConsumer {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(NotificationConsumer.class);
    public static final String CONSUMING_MESSAGE = "Consuming message: ";

    private final NotificationService notificationService;

    @KafkaListener(topics = "${notification.topic.name}", groupId = "${notification.group.id}")
    public void consume(ConsumerRecord<String, NotificationEvent> notificationRecord) {
        NotificationEvent event = notificationRecord.value();
        ExperimentOpsHeaders headers = ExperimentOpsMetadataUtil.extractHeaders(event.getMetadata());
        log.info(headers, CONSUMING_MESSAGE + event);
        notificationService.sendNotification(event, headers);
    }

}
