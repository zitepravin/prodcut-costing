package com.automotive.costing.vendorselection.repository;

import com.automotive.costing.vendorselection.entity.EvidenceStatus;
import com.automotive.costing.vendorselection.entity.SupplierQualityMetric;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierQualityMetricRepository extends JpaRepository<SupplierQualityMetric, String> {

    Optional<SupplierQualityMetric> findByCustomerIdAndMetricId(String customerId, String metricId);

    List<SupplierQualityMetric> findByCustomerIdAndSupplierIdOrderByPeriodEndDesc(String customerId, String supplierId);

    List<SupplierQualityMetric> findByCustomerIdAndSupplierIdAndEvidenceStatusOrderByPeriodEndDesc(
            String customerId, String supplierId, EvidenceStatus evidenceStatus);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    List<SupplierQualityMetric> findBySupplierIdOrderByPeriodEndDesc(String supplierId);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    List<SupplierQualityMetric> findBySupplierIdAndEvidenceStatusOrderByPeriodEndDesc(
            String supplierId, EvidenceStatus evidenceStatus);
}
