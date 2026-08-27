package com.automotive.costing.common.file;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "stored_file")
public class StoredFileEntity {

    @Id
    @Column(name = "file_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String fileId;

    @Column(name = "customer_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;

    @Column(name = "entity_type", length = 80, nullable = false)
    private String entityType;

    @Column(name = "entity_id", length = 36, columnDefinition = "CHAR(36)")
    private String entityId;

    @Column(name = "object_key", length = 512, nullable = false)
    private String objectKey;

    @Column(name = "file_name", length = 255, nullable = false)
    private String fileName;

    @Column(name = "content_type", length = 120)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

    @Column(name = "storage_provider", length = 40, nullable = false)
    private String storageProvider = "LOCAL";

    @Column(name = "status", length = 20, nullable = false)
    private String status = "ACTIVE";

    @Column(name = "created_by", length = 36)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_by", length = 36)
    private String updatedBy;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
