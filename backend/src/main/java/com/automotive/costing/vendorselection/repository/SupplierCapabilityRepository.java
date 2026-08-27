package com.automotive.costing.vendorselection.repository;

import com.automotive.costing.vendorselection.entity.CapabilityStatus;
import com.automotive.costing.vendorselection.entity.CapabilityType;
import com.automotive.costing.vendorselection.entity.SupplierCapability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierCapabilityRepository extends JpaRepository<SupplierCapability, String> {
    List<SupplierCapability> findBySupplierIdAndStatus(String supplierId, CapabilityStatus status);

    List<SupplierCapability> findBySupplierIdAndCapabilityTypeAndStatus(
            String supplierId, CapabilityType type, CapabilityStatus status);

    List<SupplierCapability> findByCapabilityCodeAndStatus(String capabilityCode, CapabilityStatus status);

    List<SupplierCapability> findByStatus(CapabilityStatus status);
    List<SupplierCapability> findByStatusAndValidToLessThanEqualOrderByValidToAsc(
            CapabilityStatus status, java.time.LocalDate validTo);

    long countByStatusAndValidToLessThanEqual(CapabilityStatus status, java.time.LocalDate validTo);
    List<SupplierCapability> findBySupplierIdOrderByCapabilityTypeAscCapabilityCodeAsc(String supplierId);
    List<SupplierCapability> findBySupplierIdAndCapabilityTypeOrderByCapabilityCodeAsc(
            String supplierId, CapabilityType type);

    long countByCustomerIdAndStatusAndValidToLessThanEqual(String customerId, CapabilityStatus status, java.time.LocalDate asOf);
}
