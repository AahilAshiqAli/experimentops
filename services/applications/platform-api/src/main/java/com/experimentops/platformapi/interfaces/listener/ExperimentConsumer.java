package com.experimentops.platformapi.interfaces.listener;

import com.experimentops.avroevent.type.EventType;
import com.experimentops.common.kafka.utils.ExperimentOpsMetadataUtil;
import com.experimentops.experiment.event.ExperimentMutationEvent;
import com.experimentops.platformapi.service.ExperimentService;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExperimentConsumer {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(ExperimentConsumer.class);
    public static final String CONSUMING_MESSAGE = "Consuming message: ";

    private final ExperimentService experimentService;

    @KafkaListener(topics = "${experiment.topic.name}", groupId = "${experiment.topic.group-id}")
    public void consume(ConsumerRecord<String, ExperimentMutationEvent> experimentRecord) {
        ExperimentMutationEvent event = experimentRecord.value();
        ExperimentOpsHeaders headers = ExperimentOpsMetadataUtil.extractHeaders(event.getMetadata());
        log.info(headers, CONSUMING_MESSAGE + event);
        EventType eventType = EventType.valueOf(event.getMetadata().getEventType());
        if (EventType.EXPERIMENT_CREATE == eventType) {
            experimentService.createExperiment(event, headers);
        } else if (EventType.EXPERIMENT_UPDATE == eventType) {
            experimentService.updateExperiment(event, headers);
        } else if (EventType.EXPERIMENT_STATUS_CHANGE == eventType) {
            experimentService.changeStatusExperiment(event, headers);
        }
    }

}
