package com.experimentops.platformapi.transformer;

import com.experimentops.common.kafka.model.event.ExperimentOpsMetadataEvent;
import com.experimentops.notification.event.NotificationEvent;
import com.experimentops.notification.event.NotificationEventPayload;
import com.experimentops.platformapi.model.entity.Notification;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import static com.experimentops.platformapi.model.type.NotificationProviderEnum.EMAIL;
import static com.experimentops.platformapi.model.type.NotificationStatusEnum.PENDING;

@Component
public class NotificationTransformer {

    private static final int FAILURE_REASON_MAX_LENGTH = 1000;

    @NonNull
    public Notification transformNotification(NotificationEvent notificationEvent) {
        ExperimentOpsMetadataEvent metadata = notificationEvent.getMetadata();
        NotificationEventPayload payload = notificationEvent.getPayload();

        Notification notification = Notification.builder()
                .eventType(metadata.getEventType())
                .workspaceUuid(metadata.getWorkspaceUuid())
                .recipient(payload.getRecipientEmail())
                .provider(EMAIL)
                .status(PENDING)
                .retryCount(0)
                .build();
        notification.setUuid(metadata.getUuid());
        return notification;
    }

    public String truncateErrorMessage(String value) {
        if (value == null || value.length() <= FAILURE_REASON_MAX_LENGTH) {
            return value;
        }
        return value.substring(0, FAILURE_REASON_MAX_LENGTH);
    }
}
