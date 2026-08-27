package com.automotive.costing.rfq.repository;

import com.automotive.costing.rfq.entity.SupplierMaster;
import com.automotive.costing.rfq.entity.SupplierStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierMasterRepository extends JpaRepository<SupplierMaster, String> {

    Optional<SupplierMaster> findByCustomerIdAndSupplierId(String customerId, String supplierId);

    Optional<SupplierMaster> findByCustomerIdAndSupplierCode(String customerId, String supplierCode);

    List<SupplierMaster> findByCustomerIdAndStatusOrderBySupplierCodeAsc(String customerId, SupplierStatus status);

    List<SupplierMaster> findByCustomerIdOrderBySupplierCodeAsc(String customerId);

    /** @deprecated Prefer tenant-scoped findByCustomerIdAndSupplierCode */
    @Deprecated
    Optional<SupplierMaster> findBySupplierCode(String supplierCode);

    /** @deprecated Prefer tenant-scoped findByCustomerIdAndSupplierId */
    @Deprecated
    Optional<SupplierMaster> findBySupplierId(String supplierId);

    /** @deprecated Prefer tenant-scoped findByCustomerIdAndStatusOrderBySupplierCodeAsc */
    @Deprecated
    List<SupplierMaster> findByStatusOrderBySupplierCodeAsc(SupplierStatus status);
}
