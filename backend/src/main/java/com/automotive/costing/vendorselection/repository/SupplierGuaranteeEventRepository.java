package com.automotive.costing.vendorselection.repository;

import com.automotive.costing.vendorselection.entity.EvidenceStatus;
import com.automotive.costing.vendorselection.entity.SupplierGuaranteeEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierGuaranteeEventRepository extends JpaRepository<SupplierGuaranteeEvent, String> {

    Optional<SupplierGuaranteeEvent> findByCustomerIdAndEventId(String customerId, String eventId);

    List<SupplierGuaranteeEvent> findByCustomerIdAndSupplierIdOrderByEventDateDesc(String customerId, String supplierId);

    List<SupplierGuaranteeEvent> findByCustomerIdAndSupplierIdAndEvidenceStatusOrderByEventDateDesc(
            String customerId, String supplierId, EvidenceStatus evidenceStatus);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    List<SupplierGuaranteeEvent> findBySupplierIdOrderByEventDateDesc(String supplierId);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    long countByResolvedFlagFalse();

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    List<SupplierGuaranteeEvent> findBySupplierIdAndResolvedFlagOrderByEventDateDesc(
            String supplierId, boolean resolvedFlag);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    List<SupplierGuaranteeEvent> findBySupplierIdAndEvidenceStatusOrderByEventDateDesc(
            String supplierId, EvidenceStatus evidenceStatus);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    List<SupplierGuaranteeEvent> findBySupplierIdAndResolvedFlagAndEvidenceStatusOrderByEventDateDesc(
            String supplierId, boolean resolvedFlag, EvidenceStatus evidenceStatus);

    long countByCustomerIdAndResolvedFlagFalse(String customerId);
}
