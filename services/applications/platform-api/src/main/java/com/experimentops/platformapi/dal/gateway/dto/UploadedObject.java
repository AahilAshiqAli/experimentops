package com.experimentops.platformapi.dal.gateway.dto;

public record UploadedObject(
        String bucketName,
        String objectKey,
        String originalFilename,
        String contentType,
        long sizeBytes
) {
}
