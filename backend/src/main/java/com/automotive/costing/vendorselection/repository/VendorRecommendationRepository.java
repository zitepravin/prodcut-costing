package com.automotive.costing.vendorselection.repository;

import com.automotive.costing.vendorselection.entity.RecommendationStatus;
import com.automotive.costing.vendorselection.entity.VendorRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VendorRecommendationRepository extends JpaRepository<VendorRecommendation, String> {

    Optional<VendorRecommendation> findByCustomerIdAndRecommendationId(String customerId, String recommendationId);

    List<VendorRecommendation> findByCustomerIdAndBomNodeIdOrderByCreatedAtDesc(String customerId, String bomNodeId);

    List<VendorRecommendation> findByCustomerIdAndStatusOrderByCreatedAtDesc(String customerId, RecommendationStatus status);

    long countByCustomerIdAndStatus(String customerId, RecommendationStatus status);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    List<VendorRecommendation> findByBomNodeIdOrderByCreatedAtDesc(String bomNodeId);

    /** @deprecated Not tenant-safe */
    @Deprecated
    long countByStatus(RecommendationStatus status);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    List<VendorRecommendation> findByStatusOrderByCreatedAtDesc(RecommendationStatus status);
}
