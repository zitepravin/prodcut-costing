package com.automotive.costing.vendorselection.repository;

import com.automotive.costing.vendorselection.entity.RiskSeverity;
import com.automotive.costing.vendorselection.entity.RiskStatus;
import com.automotive.costing.vendorselection.entity.SupplierRisk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SupplierRiskRepository extends JpaRepository<SupplierRisk, String> {

    Optional<SupplierRisk> findByCustomerIdAndRiskId(String customerId, String riskId);

    List<SupplierRisk> findByCustomerIdAndSupplierIdOrderByIdentifiedAtDesc(String customerId, String supplierId);

    List<SupplierRisk> findByCustomerIdAndSupplierIdAndStatusOrderByIdentifiedAtDesc(
            String customerId, String supplierId, RiskStatus status);

    List<SupplierRisk> findByCustomerIdAndSupplierIdAndStatusAndSeverityOrderByIdentifiedAtDesc(
            String customerId, String supplierId, RiskStatus status, RiskSeverity severity);

    long countByCustomerIdAndSupplierIdAndStatusIn(String customerId, String supplierId, Collection<RiskStatus> statuses);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    List<SupplierRisk> findBySupplierIdOrderByIdentifiedAtDesc(String supplierId);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    List<SupplierRisk> findBySupplierIdAndStatusOrderByIdentifiedAtDesc(String supplierId, RiskStatus status);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    long countBySupplierIdAndStatusIn(String supplierId, Collection<RiskStatus> statuses);

    /** @deprecated Global count — not tenant safe */
    @Deprecated
    long countByStatus(RiskStatus status);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    List<SupplierRisk> findBySupplierIdAndStatusAndSeverityOrderByIdentifiedAtDesc(
            String supplierId, RiskStatus status, RiskSeverity severity);

    long countByCustomerIdAndStatus(String customerId, RiskStatus status);
}
