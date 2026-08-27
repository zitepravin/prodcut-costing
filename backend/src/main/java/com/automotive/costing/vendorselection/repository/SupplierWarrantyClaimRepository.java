package com.automotive.costing.vendorselection.repository;

import com.automotive.costing.vendorselection.entity.EvidenceStatus;
import com.automotive.costing.vendorselection.entity.SupplierWarrantyClaim;
import com.automotive.costing.vendorselection.entity.WarrantyClaimStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierWarrantyClaimRepository extends JpaRepository<SupplierWarrantyClaim, String> {

    Optional<SupplierWarrantyClaim> findByCustomerIdAndClaimId(String customerId, String claimId);

    List<SupplierWarrantyClaim> findByCustomerIdAndSupplierIdOrderByClaimDateDesc(String customerId, String supplierId);

    List<SupplierWarrantyClaim> findByCustomerIdAndSupplierIdAndClaimStatusOrderByClaimDateDesc(
            String customerId, String supplierId, WarrantyClaimStatus claimStatus);

    List<SupplierWarrantyClaim> findByCustomerIdAndSupplierIdAndEvidenceStatusOrderByClaimDateDesc(
            String customerId, String supplierId, EvidenceStatus evidenceStatus);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    List<SupplierWarrantyClaim> findBySupplierIdOrderByClaimDateDesc(String supplierId);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    long countByClaimStatus(WarrantyClaimStatus claimStatus);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    List<SupplierWarrantyClaim> findBySupplierIdAndClaimStatusOrderByClaimDateDesc(
            String supplierId, WarrantyClaimStatus claimStatus);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    List<SupplierWarrantyClaim> findBySupplierIdAndEvidenceStatusOrderByClaimDateDesc(
            String supplierId, EvidenceStatus evidenceStatus);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    List<SupplierWarrantyClaim> findBySupplierIdAndClaimStatusAndEvidenceStatusOrderByClaimDateDesc(
            String supplierId, WarrantyClaimStatus claimStatus, EvidenceStatus evidenceStatus);

    long countByCustomerIdAndClaimStatus(String customerId, WarrantyClaimStatus claimStatus);
}
