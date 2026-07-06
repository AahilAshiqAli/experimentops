package com.experimentops.interfaces.listener;

import com.experimentops.avroevent.type.EventType;
import com.experimentops.common.kafka.utils.ExperimentOpsMetadataUtil;
import com.experimentops.dataset.version.scan.event.DatasetVersionScanRequestedEvent;
import com.experimentops.service.DatasetScanService;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DatasetScanConsumer {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(DatasetScanConsumer.class);
    public static final String CONSUMING_MESSAGE = "Consuming message: ";

    private final DatasetScanService datasetScanService;

    @KafkaListener(topics = "${dataset.version.scan.requested.topic}", groupId = "${dataset.version.scan.requested.topic.group-id}")
    public void consume(ConsumerRecord<String, DatasetVersionScanRequestedEvent> datasetVersionRecord) {
        DatasetVersionScanRequestedEvent event = datasetVersionRecord.value();
        ExperimentOpsHeaders headers = ExperimentOpsMetadataUtil.extractHeaders(event.getMetadata());
        log.info(headers, CONSUMING_MESSAGE + event);
        EventType eventType = EventType.valueOf(event.getMetadata().getEventType());
        if (EventType.DATASET_VERSION_SCAN_REQUESTED == eventType) {
            datasetScanService.startDatasetScan(event, headers);
        }
    }

}
