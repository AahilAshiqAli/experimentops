package com.experimentops.platformapi.interfaces.listener;

import com.experimentops.avroevent.type.EventType;
import com.experimentops.common.kafka.utils.ExperimentOpsMetadataUtil;
import com.experimentops.dataset.version.event.DatasetVersionMutationEvent;
import com.experimentops.dataset.version.scan.event.DatasetVersionScanCompletedEvent;
import com.experimentops.platformapi.service.DatasetService;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DatasetVersionConsumer {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(DatasetVersionConsumer.class);
    public static final String CONSUMING_MESSAGE = "Consuming message: ";

    private final DatasetService datasetService;

    @KafkaListener(topics = "${dataset.version.topic.name}", groupId = "${dataset.version.topic.group-id}")
    public void consume(ConsumerRecord<String, DatasetVersionMutationEvent> datasetVersionRecord) {
        DatasetVersionMutationEvent event = datasetVersionRecord.value();
        ExperimentOpsHeaders headers = ExperimentOpsMetadataUtil.extractHeaders(event.getMetadata());
        log.info(headers, CONSUMING_MESSAGE + event);
        EventType eventType = EventType.valueOf(event.getMetadata().getEventType());
        if (EventType.DATASET_VERSION_CREATE == eventType) {
            datasetService.createDatasetVersion(event, headers);
        } else if (EventType.DATASET_VERSION_STATUS_CHANGE == eventType) {
            datasetService.changeStatusDatasetVersion(event, headers);
        }
    }

    @KafkaListener(topics = "${dataset.version.scan.completed.topic}", groupId = "${dataset.version.scan.completed.topic.group-id}")
    public void consumeDatasetVersionScanCompleted(ConsumerRecord<String, DatasetVersionScanCompletedEvent> datasetVersionRecord) {
        DatasetVersionScanCompletedEvent event = datasetVersionRecord.value();
        ExperimentOpsHeaders headers = ExperimentOpsMetadataUtil.extractHeaders(event.getMetadata());
        log.info(headers, CONSUMING_MESSAGE + event);
        EventType eventType = EventType.valueOf(event.getMetadata().getEventType());
        if (EventType.DATASET_VERSION_SCAN_COMPLETED == eventType) {
            datasetService.processDatasetVersionScanCompleted(event, headers);
        }
    }

}
