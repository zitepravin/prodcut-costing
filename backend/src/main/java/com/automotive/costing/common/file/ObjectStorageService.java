package com.automotive.costing.common.file;

import java.io.InputStream;
import java.util.Optional;

/**
 * Abstraction for object storage (S3 / MinIO / Azure Blob / local disk).
 * MySQL holds only {@code stored_file} metadata — never large payloads.
 */
public interface ObjectStorageService {

    /**
     * Store bytes under a tenant-scoped object key.
     * @return object key written
     */
    String put(String customerId, String entityType, String entityId, String fileName,
               String contentType, InputStream data, long sizeBytes);

    Optional<StoredObject> get(String customerId, String objectKey);

    void delete(String customerId, String objectKey);

    record StoredObject(String objectKey, String contentType, long sizeBytes, InputStream content) {}
}
