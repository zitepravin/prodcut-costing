package com.automotive.costing.common.file;

import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates metadata (MySQL) + bytes (object storage) with tenant isolation.
 */
@Service
@RequiredArgsConstructor
public class StoredFileService {

    private final StoredFileRepository repository;
    private final ObjectStorageService objectStorageService;
    private final TenantAccessGuard tenantAccessGuard;

    @Value("${app.storage.provider:local}")
    private String storageProvider;

    @Transactional
    public StoredFileEntity store(String entityType, String entityId, MultipartFile file) throws IOException {
        String customerId = tenantAccessGuard.requireCustomerId();
        tenantAccessGuard.assertNotSupportReadOnly();
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is required");
        }
        String objectKey;
        try (InputStream in = file.getInputStream()) {
            objectKey = objectStorageService.put(
                    customerId, entityType, entityId, file.getOriginalFilename(),
                    file.getContentType(), in, file.getSize());
        }
        StoredFileEntity meta = new StoredFileEntity();
        meta.setFileId(UUID.randomUUID().toString());
        meta.setCustomerId(customerId);
        meta.setEntityType(entityType != null ? entityType : "UNKNOWN");
        meta.setEntityId(entityId);
        meta.setObjectKey(objectKey);
        meta.setFileName(file.getOriginalFilename() != null ? file.getOriginalFilename() : "file.bin");
        meta.setContentType(file.getContentType());
        meta.setSizeBytes(file.getSize());
        meta.setStorageProvider(storageProvider.toUpperCase());
        meta.setStatus("ACTIVE");
        meta.setCreatedAt(Instant.now());
        return repository.save(meta);
    }

    @Transactional(readOnly = true)
    public StoredFileEntity getMetadata(String fileId) {
        String customerId = tenantAccessGuard.requireCustomerId();
        return repository.findByCustomerIdAndFileId(customerId, fileId)
                .orElseThrow(() -> ResourceNotFoundException.of("StoredFile", fileId));
    }

    @Transactional(readOnly = true)
    public ObjectStorageService.StoredObject openContent(String fileId) {
        StoredFileEntity meta = getMetadata(fileId);
        return objectStorageService.get(meta.getCustomerId(), meta.getObjectKey())
                .orElseThrow(() -> ResourceNotFoundException.of("StoredFileContent", fileId));
    }

    @Transactional(readOnly = true)
    public List<StoredFileEntity> listForEntity(String entityType, String entityId) {
        String customerId = tenantAccessGuard.requireCustomerId();
        return repository.findByCustomerIdAndEntityTypeAndEntityId(customerId, entityType, entityId);
    }

    @Transactional
    public void delete(String fileId) {
        tenantAccessGuard.assertNotSupportReadOnly();
        StoredFileEntity meta = getMetadata(fileId);
        objectStorageService.delete(meta.getCustomerId(), meta.getObjectKey());
        meta.setStatus("DELETED");
        meta.setUpdatedAt(Instant.now());
        repository.save(meta);
    }
}
