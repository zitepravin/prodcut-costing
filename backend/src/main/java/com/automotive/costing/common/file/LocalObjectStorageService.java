package com.automotive.costing.common.file;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

/**
 * Local filesystem object storage for dev/single-node.
 * Production: set {@code app.storage.provider=s3} (or equivalent) and provide S3 implementation.
 * Object keys are always prefixed with customerId to enforce isolation at path level.
 */
@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
@Slf4j
public class LocalObjectStorageService implements ObjectStorageService {

    private final Path root;

    public LocalObjectStorageService(@Value("${app.storage.local-root:./data/object-storage}") String rootPath) throws IOException {
        this.root = Path.of(rootPath).toAbsolutePath().normalize();
        Files.createDirectories(this.root);
        log.info("Local object storage root={}", this.root);
    }

    @Override
    public String put(String customerId, String entityType, String entityId, String fileName,
                      String contentType, InputStream data, long sizeBytes) {
        requireTenant(customerId);
        String safeName = sanitize(fileName);
        String objectKey = customerId + "/" + nullToUnknown(entityType) + "/"
                + nullToUnknown(entityId) + "/" + UUID.randomUUID() + "_" + safeName;
        Path target = resolveUnderRoot(objectKey);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(data, target, StandardCopyOption.REPLACE_EXISTING);
            return objectKey;
        } catch (IOException e) {
            throw new BusinessRuleViolationException("Failed to store file: " + e.getMessage(), "STORAGE-01");
        }
    }

    @Override
    public Optional<StoredObject> get(String customerId, String objectKey) {
        requireTenant(customerId);
        assertKeyBelongsToCustomer(customerId, objectKey);
        Path path = resolveUnderRoot(objectKey);
        if (!Files.isRegularFile(path)) {
            return Optional.empty();
        }
        try {
            String contentType = Files.probeContentType(path);
            long size = Files.size(path);
            return Optional.of(new StoredObject(objectKey, contentType, size, Files.newInputStream(path)));
        } catch (IOException e) {
            throw new BusinessRuleViolationException("Failed to read file: " + e.getMessage(), "STORAGE-02");
        }
    }

    @Override
    public void delete(String customerId, String objectKey) {
        requireTenant(customerId);
        assertKeyBelongsToCustomer(customerId, objectKey);
        try {
            Files.deleteIfExists(resolveUnderRoot(objectKey));
        } catch (IOException e) {
            throw new BusinessRuleViolationException("Failed to delete file: " + e.getMessage(), "STORAGE-03");
        }
    }

    private Path resolveUnderRoot(String objectKey) {
        Path resolved = root.resolve(objectKey).normalize();
        if (!resolved.startsWith(root)) {
            throw new BusinessRuleViolationException("Invalid object key (path traversal)", "STORAGE-04");
        }
        return resolved;
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
        if (fileName == null || fileName.isBlank()) {
            return "file.bin";
        }
        String base = Path.of(fileName).getFileName().toString();
        return base.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static String nullToUnknown(String v) {
        return v == null || v.isBlank() ? "unknown" : v.replaceAll("[^a-zA-Z0-9_-]", "_");
    }
}
