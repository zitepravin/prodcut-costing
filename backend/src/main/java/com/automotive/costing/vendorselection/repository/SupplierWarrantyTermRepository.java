package com.automotive.costing.vendorselection.repository;

import com.automotive.costing.vendorselection.entity.SupplierWarrantyTerm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierWarrantyTermRepository extends JpaRepository<SupplierWarrantyTerm, String> {

    Optional<SupplierWarrantyTerm> findByCustomerIdAndWarrantyId(String customerId, String warrantyId);

    List<SupplierWarrantyTerm> findByCustomerIdAndSupplierIdOrderByCreatedAtDesc(String customerId, String supplierId);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    List<SupplierWarrantyTerm> findBySupplierIdOrderByCreatedAtDesc(String supplierId);

    java.util.List<SupplierWarrantyTerm> findByEvidenceStatusIn(java.util.Collection<com.automotive.costing.vendorselection.entity.EvidenceStatus> statuses);
}
