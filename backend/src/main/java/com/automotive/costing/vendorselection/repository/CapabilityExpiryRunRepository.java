package com.automotive.costing.vendorselection.repository;

import com.automotive.costing.vendorselection.entity.CapabilityExpiryRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CapabilityExpiryRunRepository extends JpaRepository<CapabilityExpiryRun, String> {
    List<CapabilityExpiryRun> findTop20ByOrderByRanAtDesc();
}
