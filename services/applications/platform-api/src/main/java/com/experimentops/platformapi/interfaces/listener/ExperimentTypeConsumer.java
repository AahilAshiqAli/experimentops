package com.experimentops.platformapi.interfaces.listener;

import com.experimentops.avroevent.type.EventType;
import com.experimentops.common.kafka.utils.ExperimentOpsMetadataUtil;
import com.experimentops.experiment.type.event.ExperimentTypeMutationEvent;
import com.experimentops.platformapi.service.ExperimentTypeService;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExperimentTypeConsumer {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(ExperimentTypeConsumer.class);
    public static final String CONSUMING_MESSAGE = "Consuming message: ";

    private final ExperimentTypeService experimentTypeService;

    @KafkaListener(topics = "${experiment.type.topic.name}", groupId = "${experiment.type.topic.group-id}")
    public void consume(ConsumerRecord<String, ExperimentTypeMutationEvent> experimentTypeRecord) {
        ExperimentTypeMutationEvent event = experimentTypeRecord.value();
        ExperimentOpsHeaders headers = ExperimentOpsMetadataUtil.extractHeaders(event.getMetadata());
        log.info(headers, CONSUMING_MESSAGE + event);
        EventType eventType = EventType.valueOf(event.getMetadata().getEventType());
        if (EventType.EXPERIMENT_TYPE_CREATE == eventType) {
            experimentTypeService.createExperimentType(event, headers);
        } else if (EventType.EXPERIMENT_TYPE_UPDATE == eventType) {
            experimentTypeService.updateExperimentType(event, headers);
        } else if (EventType.EXPERIMENT_TYPE_STATUS_CHANGE == eventType) {
            experimentTypeService.changeStatusExperimentType(event, headers);
        }
    }

}
