package com.automotive.costing.vendorselection.repository;

import com.automotive.costing.vendorselection.entity.VendorComparison;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VendorComparisonRepository extends JpaRepository<VendorComparison, String> {

    Optional<VendorComparison> findByCustomerIdAndComparisonId(String customerId, String comparisonId);

    List<VendorComparison> findByCustomerIdAndBomNodeIdOrderByCreatedAtDesc(String customerId, String bomNodeId);

    List<VendorComparison> findByCustomerIdAndProgramIdOrderByCreatedAtDesc(String customerId, String programId);

    List<VendorComparison> findTop50ByCustomerIdOrderByCreatedAtDesc(String customerId);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    List<VendorComparison> findByBomNodeIdOrderByCreatedAtDesc(String bomNodeId);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    List<VendorComparison> findByProgramIdOrderByCreatedAtDesc(String programId);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    List<VendorComparison> findTop50ByOrderByCreatedAtDesc();
}
