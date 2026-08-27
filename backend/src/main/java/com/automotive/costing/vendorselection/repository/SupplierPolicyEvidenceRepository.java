package com.automotive.costing.vendorselection.repository;

import com.automotive.costing.vendorselection.entity.EvidenceStatus;
import com.automotive.costing.vendorselection.entity.PolicyType;
import com.automotive.costing.vendorselection.entity.SupplierPolicyEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierPolicyEvidenceRepository extends JpaRepository<SupplierPolicyEvidence, String> {

    Optional<SupplierPolicyEvidence> findByCustomerIdAndPolicyId(String customerId, String policyId);

    List<SupplierPolicyEvidence> findByCustomerIdAndSupplierIdOrderByCreatedAtDesc(String customerId, String supplierId);

    List<SupplierPolicyEvidence> findByCustomerIdAndSupplierIdAndPolicyType(
            String customerId, String supplierId, PolicyType policyType);

    List<SupplierPolicyEvidence> findByCustomerIdAndSupplierIdAndEvidenceStatus(
            String customerId, String supplierId, EvidenceStatus evidenceStatus);

    long countByCustomerIdAndSupplierIdAndEvidenceStatus(
            String customerId, String supplierId, EvidenceStatus evidenceStatus);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    List<SupplierPolicyEvidence> findBySupplierIdOrderByCreatedAtDesc(String supplierId);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    List<SupplierPolicyEvidence> findBySupplierIdAndPolicyType(String supplierId, PolicyType policyType);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    List<SupplierPolicyEvidence> findBySupplierIdAndEvidenceStatus(String supplierId, EvidenceStatus evidenceStatus);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    long countBySupplierIdAndEvidenceStatus(String supplierId, EvidenceStatus evidenceStatus);

    /** @deprecated Global — not tenant safe */
    @Deprecated
    long countByEvidenceStatus(EvidenceStatus evidenceStatus);

    long countByCustomerIdAndEvidenceStatus(String customerId, EvidenceStatus evidenceStatus);

    java.util.List<com.automotive.costing.vendorselection.entity.SupplierPolicyEvidence> findByEvidenceStatusIn(java.util.Collection<com.automotive.costing.vendorselection.entity.EvidenceStatus> statuses);
}
