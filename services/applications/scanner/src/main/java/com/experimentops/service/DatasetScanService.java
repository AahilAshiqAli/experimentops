package com.experimentops.service;

import com.experimentops.common.kafka.KafkaProducer;
import com.experimentops.dataset.version.scan.event.DatasetVersionScanCompletedEvent;
import com.experimentops.dataset.version.scan.event.DatasetVersionScanRequestedEvent;
import com.experimentops.dataset.version.scan.event.DatasetVersionScanRequestedEventPayload;
import com.experimentops.objectstorage.gateway.ObjectStorageGateway;
import com.experimentops.objectstorage.gateway.dto.UploadedObject;
import com.experimentops.service.scan.DatasetFileScanRequest;
import com.experimentops.service.scan.DatasetFileScanResult;
import com.experimentops.service.scan.DatasetFileScanner;
import com.experimentops.service.scan.DatasetFileScannerRegistry;
import com.experimentops.transformer.DatasetScanEventTransformer;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import com.experimentops.validator.DatasetScanValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DatasetScanService {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(DatasetScanService.class);
    private static final String PREVIEW_JSON_FILENAME = "preview.json";
    private static final String PREVIEW_JSON_CONTENT_TYPE = "application/json";

    private final ObjectStorageGateway objectStorageGateway;
    private final DatasetFileScannerRegistry scannerRegistry;
    private final KafkaProducer kafkaProducer;
    private final DatasetScanValidator datasetScanValidator;
    private final DatasetScanEventTransformer datasetScanEventTransformer;

    @Value("${dataset.version.scan.completed.topic}")
    private String datasetVersionScanCompletedTopic;

    public void startDatasetScan(DatasetVersionScanRequestedEvent event, ExperimentOpsHeaders headers) {
        try {
            DatasetVersionScanRequestedEventPayload payload = event.getPayload();
            log.info(headers, "initiating scan request for format " + payload.getFormat());

            datasetScanValidator.validateBasicFileRules(payload);
            byte[] rawContent = objectStorageGateway.downloadObject(payload.getStorageUri());
            datasetScanValidator.validateDownloadedContent(rawContent);

            DatasetFileScanRequest request = new DatasetFileScanRequest(
                    payload.getOriginalName(),
                    payload.getFormat(),
                    payload.getStorageUri(),
                    payload.getSizeBytes(),
                    rawContent
            );
            DatasetFileScanner scanner = scannerRegistry
                    .findScanner(request)
                    .orElseThrow(() -> new IllegalArgumentException("Unsupported dataset file format"));

            DatasetFileScanResult scanResult = scanner.scan(request);
            String previewObjectKey = buildPreviewObjectKey(event, payload);
            UploadedObject uploadedPreview = objectStorageGateway.uploadObject(
                    previewObjectKey,
                    scanResult.previewJson(),
                    PREVIEW_JSON_FILENAME,
                    PREVIEW_JSON_CONTENT_TYPE,
                    headers
            );

            publishScanCompletedEvent(event, null, uploadedPreview.storageUri(), headers);
        } catch (Exception exception) {
            log.error(headers, "dataset scan failed: " + exception.getMessage(), exception);
            publishScanCompletedEvent(event, "Process failed with " + exception.getMessage(), null, headers);
        }
    }

    private String buildPreviewObjectKey(DatasetVersionScanRequestedEvent event, DatasetVersionScanRequestedEventPayload payload) {
        return "workspaces/%s/datasets/%s/versions/%s/preview/%s".formatted(
                payload.getWorkspaceUuid(),
                payload.getDatasetUuid(),
                event.getMetadata().getUuid(),
                PREVIEW_JSON_FILENAME
        );
    }

    private void publishScanCompletedEvent(
            DatasetVersionScanRequestedEvent requestEvent,
            String message,
            String previewUri,
            ExperimentOpsHeaders headers) {
        DatasetVersionScanCompletedEvent completedEvent = datasetScanEventTransformer.transformScanCompletedEvent(
                requestEvent,
                message,
                previewUri,
                headers
        );
        kafkaProducer.sendMessage(datasetVersionScanCompletedTopic, completedEvent, completedEvent.getMetadata());
    }

}
