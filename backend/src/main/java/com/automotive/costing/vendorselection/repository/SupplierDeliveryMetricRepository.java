package com.automotive.costing.vendorselection.repository;

import com.automotive.costing.vendorselection.entity.EvidenceStatus;
import com.automotive.costing.vendorselection.entity.SupplierDeliveryMetric;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierDeliveryMetricRepository extends JpaRepository<SupplierDeliveryMetric, String> {

    Optional<SupplierDeliveryMetric> findByCustomerIdAndMetricId(String customerId, String metricId);

    List<SupplierDeliveryMetric> findByCustomerIdAndSupplierIdOrderByPeriodEndDesc(String customerId, String supplierId);

    List<SupplierDeliveryMetric> findByCustomerIdAndSupplierIdAndEvidenceStatusOrderByPeriodEndDesc(
            String customerId, String supplierId, EvidenceStatus evidenceStatus);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    List<SupplierDeliveryMetric> findBySupplierIdOrderByPeriodEndDesc(String supplierId);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    List<SupplierDeliveryMetric> findBySupplierIdAndEvidenceStatusOrderByPeriodEndDesc(
            String supplierId, EvidenceStatus evidenceStatus);
}
