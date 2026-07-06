package com.experimentops.validator;

import com.experimentops.dataset.version.scan.event.DatasetVersionScanRequestedEventPayload;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DatasetScanValidator {
    private final long maxFileSizeBytes;

    public DatasetScanValidator(@Value("${dataset.scan.max-file-size-bytes:52428800}") long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public void validateBasicFileRules(DatasetVersionScanRequestedEventPayload payload) {
        if (StringUtils.isBlank(payload.getStorageUri())) {
            throw new IllegalArgumentException("Dataset version storage uri is missing");
        }
        if (StringUtils.isBlank(payload.getOriginalName())) {
            throw new IllegalArgumentException("Dataset version original file name is missing");
        }
        if (StringUtils.isBlank(payload.getDatasetUuid())) {
            throw new IllegalArgumentException("Dataset uuid is missing");
        }
        if (StringUtils.isBlank(payload.getWorkspaceUuid())) {
            throw new IllegalArgumentException("Workspace uuid is missing");
        }
        Long sizeBytes = payload.getSizeBytes();
        if (sizeBytes != null) {
            if (sizeBytes <= 0) {
                throw new IllegalArgumentException("Dataset file is empty");
            }
            if (sizeBytes > maxFileSizeBytes) {
                throw new IllegalArgumentException("Dataset file is larger than the scanner limit");
            }
        }
    }

    public void validateDownloadedContent(byte[] rawContent) {
        if (rawContent == null || rawContent.length == 0) {
            throw new IllegalArgumentException("Dataset file is empty");
        }
        if (rawContent.length > maxFileSizeBytes) {
            throw new IllegalArgumentException("Dataset file is larger than the scanner limit");
        }
    }
}
