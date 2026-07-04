package com.experimentops.platformapi.dal.gateway;

import com.experimentops.common.exceptions.constant.ErrorCode;
import com.experimentops.common.exceptions.runtime.ValidationException;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Component
public class ObjectStorageGateway {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(ObjectStorageGateway.class);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucketName;
    private final Duration uploadUrlExpiration;

    public ObjectStorageGateway(
            S3Client s3Client,
            S3Presigner s3Presigner,
            @Value("${experimentops.storage.s3.bucket}") String bucketName,
            @Value("${experimentops.storage.s3.presigned-upload-expiration-minutes:15}") long uploadUrlExpirationMinutes) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucketName = bucketName;
        this.uploadUrlExpiration = Duration.ofMinutes(uploadUrlExpirationMinutes);
    }

    public PresignedDatasetUpload createPresignedDatasetUpload(
            String workspaceId,
            String projectId,
            String datasetId,
            String datasetVersionId,
            String filename,
            ExperimentOpsHeaders headers) {
        String sanitizedFilename = sanitizeFilename(filename);
        String objectKey = buildDatasetObjectKey(
                workspaceId,
                projectId,
                datasetId,
                datasetVersionId,
                sanitizedFilename
        );

        log.info(headers, "creating presigned dataset upload for object key: " + objectKey);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(uploadUrlExpiration)
                .putObjectRequest(putObjectRequest)
                .build();
        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        Map<String, String> requiredHeaders = Map.of();

        return new PresignedDatasetUpload(
                "s3://" + bucketName + "/" + objectKey,
                presignedRequest.url().toString(),
                Instant.now().plus(uploadUrlExpiration),
                requiredHeaders
        );
    }

    public byte[] downloadFile(String objectKey) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();

        ResponseBytes<GetObjectResponse> responseBytes =
                s3Client.getObjectAsBytes(getObjectRequest);

        return responseBytes.asByteArray();
    }

    public DatasetFileMetadata getDatasetFileMetadata(String objectKey) {
        HeadObjectResponse response;
        try {
            response = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build());
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                throw new ValidationException(ErrorCode.INVALID_INPUTS, "Dataset version file was not uploaded");
            }
            throw exception;
        }
        log.info(new ExperimentOpsHeaders(), "File logging " + response );
        return new DatasetFileMetadata(response.contentLength(), response.contentType());
    }

    public void deleteFile(String objectKey) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();

        s3Client.deleteObject(deleteObjectRequest);
    }

    private String buildDatasetObjectKey(String workspaceId, String projectId, String datasetId, String datasetVersionId, String filename) {
        return "workspaces/%s/projects/%s/datasets/%s/versions/%s/raw/%s"
                .formatted(workspaceId, projectId, datasetId, datasetVersionId, filename);
    }

    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public record PresignedDatasetUpload(
            String storageUri,
            String uploadUrl,
            Instant expiresAt,
            Map<String, String> requiredHeaders) {
    }

    public record DatasetFileMetadata(long size, String contentType) {
    }

}
