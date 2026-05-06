package com.wuyou.rag.document.storage;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.MinioException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * MinIO-based implementation of {@link FileStorageService}.
 * <p>
 * Automatically initialises the configured bucket on startup.
 * File access is provided via a proxied URL through the application API.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioFileStorage implements FileStorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    @PostConstruct
    public void init() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucket).build());
                log.info("MinIO bucket created: {}", bucket);
            } else {
                log.info("MinIO bucket already exists: {}", bucket);
            }
        } catch (MinioException | InvalidKeyException | IOException | NoSuchAlgorithmException e) {
            log.error("Failed to initialise MinIO bucket: {}", bucket, e);
            throw new IllegalStateException("MinIO bucket initialisation failed", e);
        }
    }

    @Override
    public String upload(String objectName, byte[] data, String contentType) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .stream(inputStream, data.length, -1)
                            .contentType(contentType)
                            .build());
            log.info("File uploaded to MinIO: bucket={}, object={}", bucket, objectName);
            return objectName;
        } catch (MinioException | InvalidKeyException | IOException | NoSuchAlgorithmException e) {
            log.error("Failed to upload file to MinIO: object={}", objectName, e);
            throw new RuntimeException("File upload failed", e);
        }
    }

    @Override
    public byte[] download(String objectName) {
        try {
            GetObjectArgs args = GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build();
            try (InputStream stream = minioClient.getObject(args)) {
                return stream.readAllBytes();
            }
        } catch (MinioException | InvalidKeyException | IOException | NoSuchAlgorithmException e) {
            log.error("Failed to download file from MinIO: object={}", objectName, e);
            throw new RuntimeException("File download failed", e);
        }
    }

    @Override
    public void delete(String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build());
            log.info("File deleted from MinIO: bucket={}, object={}", bucket, objectName);
        } catch (MinioException | InvalidKeyException | IOException | NoSuchAlgorithmException e) {
            log.error("Failed to delete file from MinIO: object={}", objectName, e);
            throw new RuntimeException("File deletion failed", e);
        }
    }

    @Override
    public String getUrl(String objectName) {
        return "/api/v1/files/" + objectName;
    }
}
