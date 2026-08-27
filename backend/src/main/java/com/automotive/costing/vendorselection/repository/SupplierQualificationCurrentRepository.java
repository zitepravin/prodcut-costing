package com.automotive.costing.vendorselection.repository;

import com.automotive.costing.vendorselection.entity.SupplierLifecycleStatus;
import com.automotive.costing.vendorselection.entity.SupplierQualificationCurrent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierQualificationCurrentRepository extends JpaRepository<SupplierQualificationCurrent, String> {

    Optional<SupplierQualificationCurrent> findByCustomerIdAndSupplierId(String customerId, String supplierId);

    List<SupplierQualificationCurrent> findByCustomerIdAndLifecycleStatus(String customerId, SupplierLifecycleStatus status);

    List<SupplierQualificationCurrent> findByCustomerId(String customerId);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    List<SupplierQualificationCurrent> findByLifecycleStatus(SupplierLifecycleStatus status);
}
