package com.automotive.costing.vendorselection.repository;

import com.automotive.costing.vendorselection.entity.EvidenceStatus;
import com.automotive.costing.vendorselection.entity.SupplierAfterSalesMetric;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierAfterSalesMetricRepository extends JpaRepository<SupplierAfterSalesMetric, String> {

    Optional<SupplierAfterSalesMetric> findByCustomerIdAndMetricId(String customerId, String metricId);

    List<SupplierAfterSalesMetric> findByCustomerIdAndSupplierIdOrderByPeriodEndDesc(String customerId, String supplierId);

    List<SupplierAfterSalesMetric> findByCustomerIdAndSupplierIdAndEvidenceStatusOrderByPeriodEndDesc(
            String customerId, String supplierId, EvidenceStatus evidenceStatus);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    List<SupplierAfterSalesMetric> findBySupplierIdOrderByPeriodEndDesc(String supplierId);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    List<SupplierAfterSalesMetric> findBySupplierIdAndEvidenceStatusOrderByPeriodEndDesc(
            String supplierId, EvidenceStatus evidenceStatus);
}
