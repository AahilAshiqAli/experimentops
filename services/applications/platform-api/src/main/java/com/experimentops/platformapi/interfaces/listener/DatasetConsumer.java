package com.experimentops.platformapi.interfaces.listener;

import com.experimentops.avroevent.type.EventType;
import com.experimentops.common.kafka.utils.ExperimentOpsMetadataUtil;
import com.experimentops.dataset.event.DatasetMutationEvent;
import com.experimentops.platformapi.service.DatasetService;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DatasetConsumer {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(DatasetConsumer.class);
    public static final String CONSUMING_MESSAGE = "Consuming message: ";

    private final DatasetService datasetService;

    @KafkaListener(topics = "${dataset.topic.name}", groupId = "${dataset.topic.group-id}")
    public void consume(ConsumerRecord<String, DatasetMutationEvent> datasetRecord) {
        DatasetMutationEvent event = datasetRecord.value();
        ExperimentOpsHeaders headers = ExperimentOpsMetadataUtil.extractHeaders(event.getMetadata());
        log.info(headers, CONSUMING_MESSAGE + event);
        EventType eventType = EventType.valueOf(event.getMetadata().getEventType());
        if (EventType.DATASET_CREATE == eventType) {
            datasetService.createDataset(event, headers);
        } else if (EventType.DATASET_UPDATE == eventType) {
            datasetService.updateDataset(event, headers);
        } else if (EventType.DATASET_STATUS_CHANGE == eventType) {
            datasetService.changeStatusDataset(event, headers);
        }
    }

}
