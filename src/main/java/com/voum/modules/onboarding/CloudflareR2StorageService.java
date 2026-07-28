package com.voum.modules.onboarding;

import com.voum.common.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.util.UUID;

/**
 * StorageService implementation supporting Cloudflare R2 via official AWS S3 Compatible API.
 */
@Service
@Primary
public class CloudflareR2StorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(CloudflareR2StorageService.class);
    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB

    @Value("${app.storage.type:local}")
    private String storageType;

    @Value("${app.storage.r2.account-id:}")
    private String accountId;

    @Value("${app.storage.r2.access-key-id:}")
    private String accessKeyId;

    @Value("${app.storage.r2.secret-access-key:}")
    private String secretAccessKey;

    @Value("${app.storage.r2.bucket-name:voum-data}")
    private String bucketName;

    @Value("${app.storage.r2.public-domain:}")
    private String publicDomain;

    private final LocalStorageService localStorageService;

    public CloudflareR2StorageService(LocalStorageService localStorageService) {
        this.localStorageService = localStorageService;
    }

    @Override
    public StoredFile uploadFile(String folder, String originalFilename, byte[] content, String contentType) {
        if (!"r2".equalsIgnoreCase(storageType) || accountId == null || accountId.trim().isEmpty()
                || accessKeyId == null || accessKeyId.trim().isEmpty()) {
            log.info("R2 storage credentials incomplete or storage type is local. Falling back to LocalStorageService.");
            return localStorageService.uploadFile(folder, originalFilename, content, contentType);
        }

        if (content == null || content.length == 0) {
            throw new ApiException("File content cannot be empty.", HttpStatus.BAD_REQUEST);
        }

        if (content.length > MAX_FILE_SIZE_BYTES) {
            throw new ApiException("File size exceeds maximum limit of 10MB.", HttpStatus.BAD_REQUEST);
        }

        String extension = getFileExtension(originalFilename);
        String uniqueFilename = UUID.randomUUID() + "." + extension;
        String key = folder + "/" + uniqueFilename;

        try {
            String endpoint = "https://" + accountId + ".r2.cloudflarestorage.com";
            
            S3Client s3Client = S3Client.builder()
                    .endpointOverride(URI.create(endpoint))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKeyId, secretAccessKey)
                    ))
                    .region(Region.of("auto"))
                    .build();

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType != null ? contentType : "application/octet-stream")
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(content));
            s3Client.close();

            log.info("Successfully uploaded object to Cloudflare R2 bucket '{}' with key '{}'", bucketName, key);

            String fileUrl;
            if (publicDomain != null && !publicDomain.trim().isEmpty()) {
                fileUrl = publicDomain.endsWith("/") ? publicDomain + key : publicDomain + "/" + key;
            } else {
                fileUrl = endpoint + "/" + bucketName + "/" + key;
            }

            return new StoredFile(fileUrl, uniqueFilename, contentType, (long) content.length);
        } catch (Exception e) {
            log.error("Failed to upload to Cloudflare R2: {}", e.getMessage(), e);
            log.warn("Falling back to local disk storage for key: {}", key);
            return localStorageService.uploadFile(folder, originalFilename, content, contentType);
        }
    }

    @Override
    public byte[] downloadFile(String fileUrl) {
        return localStorageService.downloadFile(fileUrl);
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "jpg";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}
