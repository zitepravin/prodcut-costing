package com.automotive.costing.common.file;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoredFileRepository extends JpaRepository<StoredFileEntity, String> {

    Optional<StoredFileEntity> findByCustomerIdAndFileId(String customerId, String fileId);

    Optional<StoredFileEntity> findByCustomerIdAndObjectKey(String customerId, String objectKey);

    List<StoredFileEntity> findByCustomerIdAndEntityTypeAndEntityId(
            String customerId, String entityType, String entityId);
}
