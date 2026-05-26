package com.experimentops.common.kafka.utils;

import com.experimentops.common.exceptions.ExperimentOpsException;
import com.experimentops.common.exceptions.constant.ErrorCode;
import com.experimentops.common.kafka.constant.KafkaConstants;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.kafka.core.KafkaOperations;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public final class DLTExceptionUtil {

    private DLTExceptionUtil() {}

    public static DeadLetterPublishingRecoverer getDeadLetterPublishingRecoverer(
            KafkaOperations<String, Object> kafkaOperations) {

        DeadLetterPublishingRecoverer dlpr = new DeadLetterPublishingRecoverer(kafkaOperations);
        AtomicReference<String> exceptionMessage = new AtomicReference<>(KafkaConstants.NOT_AVAILABLE);

        dlpr.setHeadersFunction((consumerRecord, exception) -> {
            exceptionMessage.set(exception.getMessage());
            int errorCode = ErrorCode.GLOBAL_ERROR.getCode();

            if (exception instanceof ListenerExecutionFailedException lefe) {
                if (lefe.getRootCause() instanceof ExperimentOpsException experimentOpsException) {
                    errorCode = experimentOpsException.getErrorCode().getCode();
                }
            }

            // forward tracing headers to DLT so you can trace the original request
            Stream.of(consumerRecord.headers().toArray()).forEach(h -> {
                if (KafkaConstants.REQUEST_UUID.equals(h.key())) {
                    consumerRecord.headers().add(KafkaConstants.REQUEST_UUID,
                            new String(h.value()).getBytes(StandardCharsets.UTF_8));
                }
                if (KafkaConstants.WORKSPACE_UUID.equals(h.key())) {
                    consumerRecord.headers().add(KafkaConstants.WORKSPACE_UUID,
                            new String(h.value()).getBytes(StandardCharsets.UTF_8));
                }
            });

            consumerRecord.headers().add(KafkaConstants.ERROR_CODE,
                    String.valueOf(errorCode).getBytes(StandardCharsets.UTF_8));
            consumerRecord.headers().add(KafkaConstants.EXCEPTION_MESSAGE,
                    exceptionMessage.get().getBytes(StandardCharsets.UTF_8));

            return consumerRecord.headers();
        });

        return dlpr;
    }
}
