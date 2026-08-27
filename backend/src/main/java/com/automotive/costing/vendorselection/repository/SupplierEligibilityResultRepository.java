package com.automotive.costing.vendorselection.repository;

import com.automotive.costing.vendorselection.entity.SupplierEligibilityResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierEligibilityResultRepository extends JpaRepository<SupplierEligibilityResult, String> {
    List<SupplierEligibilityResult> findByBomNodeIdOrderByEvaluatedAtDesc(String bomNodeId);
}
