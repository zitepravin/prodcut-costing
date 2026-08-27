package com.automotive.costing.vendorselection.repository;

import com.automotive.costing.vendorselection.entity.SupplierGuaranteeTerm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierGuaranteeTermRepository extends JpaRepository<SupplierGuaranteeTerm, String> {

    Optional<SupplierGuaranteeTerm> findByCustomerIdAndGuaranteeId(String customerId, String guaranteeId);

    List<SupplierGuaranteeTerm> findByCustomerIdAndSupplierIdOrderByCreatedAtDesc(String customerId, String supplierId);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    List<SupplierGuaranteeTerm> findBySupplierIdOrderByCreatedAtDesc(String supplierId);

    java.util.List<SupplierGuaranteeTerm> findByEvidenceStatusIn(java.util.Collection<com.automotive.costing.vendorselection.entity.EvidenceStatus> statuses);
}
