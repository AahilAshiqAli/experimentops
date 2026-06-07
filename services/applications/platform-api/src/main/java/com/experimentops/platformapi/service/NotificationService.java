package com.experimentops.platformapi.service;

import com.experimentops.notification.event.NotificationEvent;
import com.experimentops.notification.event.NotificationEventPayload;
import com.experimentops.platformapi.dal.gateway.EmailGateway;
import com.experimentops.platformapi.dal.repository.NotificationRepository;
import com.experimentops.platformapi.model.entity.Notification;
import com.experimentops.platformapi.transformer.NotificationTransformer;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import static com.experimentops.platformapi.model.type.NotificationStatusEnum.FAILED;
import static com.experimentops.platformapi.model.type.NotificationStatusEnum.SUCCESSFUL;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final EmailGateway emailGateway;
    private final NotificationRepository notificationRepository;
    private final NotificationTransformer notificationTransformer;

    public void sendNotification(@NonNull NotificationEvent notificationEvent, @NonNull ExperimentOpsHeaders headers) {
        Notification notification = createOrGetNotification(notificationEvent);
        if (SUCCESSFUL == notification.getStatus()) {
            return;
        }

        NotificationEventPayload payload = notificationEvent.getPayload();
        notification.setRetryCount(notification.getRetryCount() + 1);
        notification.setFailureReason(null);
        notificationRepository.save(notification);

        try {
            sendEmailNotification(payload, headers);
            notification.setStatus(SUCCESSFUL);
            notificationRepository.save(notification);
        } catch (RuntimeException exception) {
            notification.setFailureReason(notificationTransformer.truncateErrorMessage(exception.getMessage()));
            notificationRepository.save(notification);
            throw exception;
        }
    }

    public void markNotificationFailed(@NonNull NotificationEvent notificationEvent, String failureReason) {
        Notification notification = createOrGetNotification(notificationEvent);
        notification.setStatus(FAILED);
        notification.setFailureReason(notificationTransformer.truncateErrorMessage(failureReason));
        notificationRepository.save(notification);
    }

    private void sendEmailNotification(NotificationEventPayload payload, ExperimentOpsHeaders headers) {
        emailGateway.sendSimpleNotification(
                payload.getRecipientEmail(),
                payload.getSubject(),
                payload.getBody(),
                headers
        );
    }

    private Notification createOrGetNotification(NotificationEvent notificationEvent) {
        String notificationUuid = notificationEvent.getMetadata().getUuid();
        return notificationRepository
                .findByUuidAndEnabled(notificationUuid, true)
                .orElseGet(() -> saveNewNotification(notificationEvent));
    }

    private Notification saveNewNotification(NotificationEvent notificationEvent) {
        try {
            return notificationRepository.save(notificationTransformer.transformNotification(notificationEvent));
        } catch (DataIntegrityViolationException exception) { // Not really needed. Only for race condition.
            return notificationRepository
                    .findByUuidAndEnabled(notificationEvent.getMetadata().getUuid(), true)
                    .orElseThrow(() -> exception);
        }
    }
}
