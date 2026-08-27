package com.automotive.costing.vendorselection.repository;

import com.automotive.costing.vendorselection.entity.SupplierTcoCalculation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierTcoCalculationRepository extends JpaRepository<SupplierTcoCalculation, String> {

    Optional<SupplierTcoCalculation> findByCustomerIdAndTcoId(String customerId, String tcoId);

    List<SupplierTcoCalculation> findByCustomerIdAndSupplierIdOrderByCalculatedAtDesc(String customerId, String supplierId);

    List<SupplierTcoCalculation> findByCustomerIdAndBomNodeIdOrderByCalculatedAtDesc(String customerId, String bomNodeId);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    List<SupplierTcoCalculation> findBySupplierIdOrderByCalculatedAtDesc(String supplierId);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    List<SupplierTcoCalculation> findByBomNodeIdOrderByCalculatedAtDesc(String bomNodeId);
}
