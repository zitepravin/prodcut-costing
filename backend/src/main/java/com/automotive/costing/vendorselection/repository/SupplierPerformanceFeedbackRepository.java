package com.automotive.costing.vendorselection.repository;

import com.automotive.costing.vendorselection.entity.EvidenceStatus;
import com.automotive.costing.vendorselection.entity.SupplierPerformanceFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierPerformanceFeedbackRepository extends JpaRepository<SupplierPerformanceFeedback, String> {
    List<SupplierPerformanceFeedback> findBySupplierIdOrderByFeedbackDateDesc(String supplierId);
    long countByEvidenceStatus(EvidenceStatus evidenceStatus);
    List<SupplierPerformanceFeedback> findBySupplierIdAndAppliedToHistoryOrderByFeedbackDateDesc(
            String supplierId, boolean appliedToHistory);
    List<SupplierPerformanceFeedback> findBySupplierIdAndEvidenceStatusOrderByFeedbackDateDesc(
            String supplierId, EvidenceStatus evidenceStatus);

    long countByCustomerIdAndEvidenceStatus(String customerId, EvidenceStatus evidenceStatus);
}
