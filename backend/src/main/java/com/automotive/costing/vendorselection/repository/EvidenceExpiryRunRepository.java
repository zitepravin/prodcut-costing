package com.automotive.costing.vendorselection.repository;

import com.automotive.costing.vendorselection.entity.EvidenceExpiryRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvidenceExpiryRunRepository extends JpaRepository<EvidenceExpiryRun, String> {
    List<EvidenceExpiryRun> findTop20ByOrderByRanAtDesc();
}
