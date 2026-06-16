package com.experimentops.platformapi.dal.gateway.dto;

public record UploadedObject(
        String bucketName,
        String objectKey,
        String originalFilename,
        String contentType,
        long sizeBytes
) {
    public String storageUri() {
        return "s3://%s/%s".formatted(bucketName, objectKey);
    }
}
