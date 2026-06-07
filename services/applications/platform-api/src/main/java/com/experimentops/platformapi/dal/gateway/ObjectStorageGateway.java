package com.experimentops.platformapi.dal.gateway;

import com.experimentops.platformapi.dal.gateway.dto.UploadedObject;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;

@Component
public class ObjectStorageGateway {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(ObjectStorageGateway.class);

    private final S3Client s3Client;
    private final String bucketName;

    public ObjectStorageGateway(S3Client s3Client,
            @Value("${experimentops.storage.s3.bucket}") String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    public UploadedObject uploadDatasetFile(String workspaceId, String projectId, String datasetId, MultipartFile multipartFile, ExperimentOpsHeaders headers) {
        validateMultipartFile(multipartFile);

        String originalFilename = getOriginalFilename(multipartFile);
        String sanitizedFilename = sanitizeFilename(originalFilename);

        String objectKey = buildDatasetObjectKey(
                workspaceId,
                projectId,
                datasetId,
                sanitizedFilename
        );

        log.info(headers, "uploading dataset with object key: " + objectKey );

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType(multipartFile.getContentType())
                    .contentLength(multipartFile.getSize())
                    .build();

            s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromInputStream(
                            multipartFile.getInputStream(),
                            multipartFile.getSize()
                    )
            );

            return new UploadedObject(bucketName, objectKey,
                    originalFilename, multipartFile.getContentType(),
                    multipartFile.getSize()
            );

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to read dataset file before uploading to object storage",
                    exception
            );
        }
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

    public void deleteFile(String objectKey) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();

        s3Client.deleteObject(deleteObjectRequest);
    }

    private String buildDatasetObjectKey(String workspaceId, String projectId, String datasetId, String filename) {
        return "workspaces/%s/projects/%s/datasets/%s/raw/%s"
                .formatted(workspaceId, projectId, datasetId, filename);
    }

    private void validateMultipartFile(MultipartFile multipartFile) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new IllegalArgumentException("Dataset file is required");
        }
    }

    private String getOriginalFilename(MultipartFile multipartFile) {
        String originalFilename = multipartFile.getOriginalFilename();

        if (originalFilename == null || originalFilename.isBlank()) {
            return "dataset-file";
        }

        return originalFilename;
    }

    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

}