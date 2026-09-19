package com.trackforge.bug.service;

import io.minio.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class StorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    public StorageService(
            @Value("${minio.url}") String url,
            @Value("${minio.access-key}") String accessKey,
            @Value("${minio.secret-key}") String secretKey) {
        this.minioClient = MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey, secretKey)
                .build();
        ensureBucketExists();
    }

    private void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Created MinIO bucket: {}", bucket);
            }
        } catch (Exception e) {
            log.error("Failed to ensure bucket exists: {}", e.getMessage());
        }
    }

    public String upload(MultipartFile file, String folder) {
        try {
            String filename = folder + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(filename)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            log.info("Uploaded file: {}", filename);
            return filename;
        } catch (Exception e) {
            throw new RuntimeException("File upload failed: " + e.getMessage(), e);
        }
    }

    public String generatePresignedUrl(String storedFilename) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(bucket)
                            .object(storedFilename)
                            .method(Method.GET)
                            .expiry(1, TimeUnit.HOURS)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to generate presigned URL: {}", e.getMessage());
            return null;
        }
    }

    public void delete(String storedFilename) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder().bucket(bucket).object(storedFilename).build()
            );
        } catch (Exception e) {
            log.error("Failed to delete file {}: {}", storedFilename, e.getMessage());
        }
    }
}