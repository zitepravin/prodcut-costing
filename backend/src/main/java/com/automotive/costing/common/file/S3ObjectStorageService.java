package com.automotive.costing.common.file;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

/**
 * S3 / MinIO-compatible object storage. Enabled when {@code app.storage.provider=s3}.
 * Object keys are always prefixed with customerId.
 */
@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "s3")
@Slf4j
public class S3ObjectStorageService implements ObjectStorageService {

    private final S3Client s3Client;
    private final String bucket;

    public S3ObjectStorageService(
            S3Client s3Client,
            @Value("${app.storage.s3.bucket:product-costing-files}") String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        log.info("S3 object storage bucket={}", bucket);
    }

    @Override
    public String put(String customerId, String entityType, String entityId, String fileName,
                      String contentType, InputStream data, long sizeBytes) {
        requireTenant(customerId);
        String safeName = sanitize(fileName);
        String objectKey = customerId + "/" + nullToUnknown(entityType) + "/"
                + nullToUnknown(entityId) + "/" + UUID.randomUUID() + "_" + safeName;
        try {
            PutObjectRequest.Builder b = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey);
            if (contentType != null && !contentType.isBlank()) {
                b.contentType(contentType);
            }
            s3Client.putObject(b.build(), RequestBody.fromInputStream(data, sizeBytes));
            return objectKey;
        } catch (Exception e) {
            throw new BusinessRuleViolationException("S3 put failed: " + e.getMessage(), "STORAGE-S3-01");
        }
    }

    @Override
    public Optional<StoredObject> get(String customerId, String objectKey) {
        requireTenant(customerId);
        assertKeyBelongsToCustomer(customerId, objectKey);
        try {
            var resp = s3Client.getObject(GetObjectRequest.builder().bucket(bucket).key(objectKey).build());
            String ct = resp.response().contentType();
            long size = resp.response().contentLength() != null ? resp.response().contentLength() : -1L;
            return Optional.of(new StoredObject(objectKey, ct, size, resp));
        } catch (Exception e) {
            log.warn("S3 get miss or error key={}: {}", objectKey, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void delete(String customerId, String objectKey) {
        requireTenant(customerId);
        assertKeyBelongsToCustomer(customerId, objectKey);
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(objectKey).build());
        } catch (Exception e) {
            throw new BusinessRuleViolationException("S3 delete failed: " + e.getMessage(), "STORAGE-S3-03");
        }
    }

    private static void requireTenant(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            throw new BusinessRuleViolationException("customerId required for object storage", "STORAGE-05");
        }
    }

    private static void assertKeyBelongsToCustomer(String customerId, String objectKey) {
        if (objectKey == null || !objectKey.startsWith(customerId + "/")) {
            throw new BusinessRuleViolationException("Object key not owned by tenant", "STORAGE-06");
        }
    }

    private static String sanitize(String fileName) {
        if (fileName == null || fileName.isBlank()) return "file.bin";
        String base = fileName.replace("\\", "/");
        int slash = base.lastIndexOf('/');
        if (slash >= 0) base = base.substring(slash + 1);
        return base.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static String nullToUnknown(String v) {
        return v == null || v.isBlank() ? "unknown" : v.replaceAll("[^a-zA-Z0-9_-]", "_");
    }
}
