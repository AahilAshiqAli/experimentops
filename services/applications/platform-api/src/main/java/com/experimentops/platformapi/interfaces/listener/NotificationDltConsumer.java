package com.experimentops.platformapi.interfaces.listener;

import com.experimentops.common.kafka.constant.KafkaConstants;
import com.experimentops.common.kafka.utils.ExperimentOpsMetadataUtil;
import com.experimentops.notification.event.NotificationEvent;
import com.experimentops.platformapi.service.NotificationService;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class NotificationDltConsumer {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(NotificationDltConsumer.class);
    public static final String CONSUMING_DLT_MESSAGE = "Consuming notification DLT message: ";

    private final NotificationService notificationService;

    @KafkaListener(topics = "${notification.dlt.topic.name}", groupId = "${notification.group.id}")
    public void consume(ConsumerRecord<String, NotificationEvent> notificationRecord) {
        NotificationEvent event = notificationRecord.value();
        ExperimentOpsHeaders headers = ExperimentOpsMetadataUtil.extractHeaders(event.getMetadata());
        log.error(headers, CONSUMING_DLT_MESSAGE + event);
        notificationService.markNotificationFailed(event, extractFailureReason(notificationRecord));
    }

    private String extractFailureReason(ConsumerRecord<String, NotificationEvent> notificationRecord) {
        Header exceptionMessage = notificationRecord.headers().lastHeader(KafkaConstants.EXCEPTION_MESSAGE);
        if (exceptionMessage == null || exceptionMessage.value() == null) {
            return KafkaConstants.NOT_AVAILABLE;
        }
        return new String(exceptionMessage.value(), StandardCharsets.UTF_8);
    }
}
