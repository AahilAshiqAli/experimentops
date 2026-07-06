package com.experimentops.transformer;

import com.experimentops.avroevent.type.EventType;
import com.experimentops.common.kafka.model.event.ExperimentOpsMetadataEvent;
import com.experimentops.common.kafka.utils.ExperimentOpsMetadataUtil;
import com.experimentops.dataset.version.scan.event.DatasetVersionScanCompletedEvent;
import com.experimentops.dataset.version.scan.event.DatasetVersionScanCompletedEventPayload;
import com.experimentops.dataset.version.scan.event.DatasetVersionScanRequestedEvent;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import org.springframework.stereotype.Component;

@Component
public class DatasetScanEventTransformer {

    public DatasetVersionScanCompletedEvent transformScanCompletedEvent(
            DatasetVersionScanRequestedEvent requestEvent,
            String message,
            String previewUri,
            ExperimentOpsHeaders headers) {
        ExperimentOpsMetadataEvent metadata = ExperimentOpsMetadataUtil.metadataEvent(
                headers,
                requestEvent.getMetadata().getUuid(),
                EventType.DATASET_VERSION_SCAN_COMPLETED.name(),
                this.getClass().getSimpleName()
        );
        DatasetVersionScanCompletedEventPayload payload = DatasetVersionScanCompletedEventPayload.newBuilder()
                .setMessage(message)
                .setPreviewUri(previewUri)
                .build();
        return DatasetVersionScanCompletedEvent.newBuilder()
                .setMetadata(metadata)
                .setPayload(payload)
                .build();
    }
}
