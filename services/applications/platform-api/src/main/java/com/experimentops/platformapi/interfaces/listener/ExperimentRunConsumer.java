package com.experimentops.platformapi.interfaces.listener;

import com.experimentops.common.kafka.utils.ExperimentOpsMetadataUtil;
import com.experimentops.experiment.run.event.ExperimentRunCompletedEvent;
import com.experimentops.experiment.run.event.ExperimentRunFailureEvent;
import com.experimentops.experiment.run.event.ExperimentRunProgressEvent;
import com.experimentops.platformapi.service.ExperimentRunService;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExperimentRunConsumer {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(ExperimentRunConsumer.class);
    public static final String CONSUMING_MESSAGE = "Consuming message: ";

    private final ExperimentRunService experimentRunService;

    @KafkaListener(topics = "${experiment.run.completed.topic}", groupId = "${experiment.run.completed.topic.group-id}")
    public void consumeExperimentRunCompleted(ConsumerRecord<String, ExperimentRunCompletedEvent> experimentRunRecord) {
        ExperimentRunCompletedEvent event = experimentRunRecord.value();
        ExperimentOpsHeaders headers = ExperimentOpsMetadataUtil.extractHeaders(event.getMetadata());
        log.info(headers, CONSUMING_MESSAGE + event);
        experimentRunService.processExperimentRunCompleted(event, headers);
    }

    @KafkaListener(topics = "${experiment.run.failure.topic}", groupId = "${experiment.run.failure.topic.group-id}")
    public void consumeExperimentRunFailure(ConsumerRecord<String, ExperimentRunFailureEvent> experimentRunRecord) {
        ExperimentRunFailureEvent event = experimentRunRecord.value();
        ExperimentOpsHeaders headers = ExperimentOpsMetadataUtil.extractHeaders(event.getMetadata());
        log.info(headers, CONSUMING_MESSAGE + event);
        experimentRunService.processExperimentRunFailure(event, headers);
    }

    @KafkaListener(topics = "${experiment.run.progress.topic}", groupId = "${experiment.run.progress.topic.group-id}", concurrency = "3")
    public void consumeExperimentRunProgress(ConsumerRecord<String, ExperimentRunProgressEvent> experimentRunRecord) {
        ExperimentRunProgressEvent event = experimentRunRecord.value();
        ExperimentOpsHeaders headers = ExperimentOpsMetadataUtil.extractHeaders(event.getMetadata());
        log.info(headers, CONSUMING_MESSAGE + event);
        experimentRunService.processExperimentRunProgress(event, headers);
    }

}
