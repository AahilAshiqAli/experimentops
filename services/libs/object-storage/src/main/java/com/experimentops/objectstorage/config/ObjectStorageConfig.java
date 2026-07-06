package com.experimentops.objectstorage.config;

import com.experimentops.objectstorage.gateway.ObjectStorageGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class ObjectStorageConfig {

    @Bean
    @ConditionalOnProperty(name = "experimentops.storage.s3.region")
    public S3Client s3Client(@Value("${experimentops.storage.s3.region}") String region) {
        return S3Client.builder()
                .region(Region.of(region))
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "experimentops.storage.s3.region")
    public S3Presigner s3Presigner(@Value("${experimentops.storage.s3.region}") String region) {
        return S3Presigner.builder()
                .region(Region.of(region))
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = {
            "experimentops.storage.s3.bucket",
            "experimentops.storage.s3.region"
    })
    public ObjectStorageGateway objectStorageGateway(
            S3Client s3Client,
            S3Presigner s3Presigner,
            @Value("${experimentops.storage.s3.bucket}") String bucketName,
            @Value("${experimentops.storage.s3.presigned-upload-expiration-minutes:15}") long uploadUrlExpirationMinutes) {
        return new ObjectStorageGateway(s3Client, s3Presigner, bucketName, uploadUrlExpirationMinutes);
    }
}
