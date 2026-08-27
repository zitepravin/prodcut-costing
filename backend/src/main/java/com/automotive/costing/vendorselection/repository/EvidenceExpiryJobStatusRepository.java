package com.automotive.costing.vendorselection.repository;

import com.automotive.costing.vendorselection.entity.EvidenceExpiryJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EvidenceExpiryJobStatusRepository extends JpaRepository<EvidenceExpiryJobStatus, String> {
    Optional<EvidenceExpiryJobStatus> findByJobName(String jobName);
}
