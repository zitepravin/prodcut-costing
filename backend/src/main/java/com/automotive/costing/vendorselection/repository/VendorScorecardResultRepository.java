package com.automotive.costing.vendorselection.repository;

import com.automotive.costing.vendorselection.entity.VendorScorecardResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VendorScorecardResultRepository extends JpaRepository<VendorScorecardResult, String> {

    Optional<VendorScorecardResult> findByCustomerIdAndResultId(String customerId, String resultId);

    List<VendorScorecardResult> findByCustomerIdAndSupplierIdOrderByScoredAtDesc(String customerId, String supplierId);

    long countByCustomerIdAndTemplateId(String customerId, String templateId);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    List<VendorScorecardResult> findBySupplierIdOrderByScoredAtDesc(String supplierId);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    long countByTemplateId(String templateId);
}
