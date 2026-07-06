package com.experimentops.objectstorage.gateway;

import com.experimentops.common.exceptions.constant.ErrorCode;
import com.experimentops.common.exceptions.runtime.ValidationException;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.lang.reflect.Proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ObjectStorageGatewayTest {

    @Test
    void getDatasetFileMetadata_whenObjectDoesNotExist_throwsFileNotUploadedException() {
        S3Client s3Client = s3ClientThrowingNotFound();
        ObjectStorageGateway gateway = new ObjectStorageGateway(s3Client, null, "test-bucket", 15);

        assertThatThrownBy(() -> gateway.getDatasetFileMetadata("missing.csv"))
                .isInstanceOfSatisfying(ValidationException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUTS);
                    assertThat(exception).hasMessage("Dataset version file was not uploaded");
                });
    }

    private S3Client s3ClientThrowingNotFound() {
        return (S3Client) Proxy.newProxyInstance(
                S3Client.class.getClassLoader(),
                new Class<?>[]{S3Client.class},
                (proxy, method, args) -> {
                    if ("headObject".equals(method.getName())) {
                        throw S3Exception.builder().statusCode(404).message("Not Found").build();
                    }
                    if ("serviceName".equals(method.getName())) {
                        return "s3";
                    }
                    if ("close".equals(method.getName())) {
                        return null;
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
    }
}
