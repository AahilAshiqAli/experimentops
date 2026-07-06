package com.experimentops.service.scan;

public record DatasetFileScanRequest(
        String originalName,
        String format,
        String storageUri,
        Long sizeBytes,
        byte[] content
) {
}
