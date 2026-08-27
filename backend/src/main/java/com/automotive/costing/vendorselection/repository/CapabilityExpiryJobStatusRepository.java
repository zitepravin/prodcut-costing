package com.automotive.costing.vendorselection.repository;

import com.automotive.costing.vendorselection.entity.CapabilityExpiryJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CapabilityExpiryJobStatusRepository extends JpaRepository<CapabilityExpiryJobStatus, String> {
    Optional<CapabilityExpiryJobStatus> findByJobName(String jobName);
}
