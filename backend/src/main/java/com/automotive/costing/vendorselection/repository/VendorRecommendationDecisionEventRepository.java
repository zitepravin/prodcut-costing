package com.automotive.costing.vendorselection.repository;

import com.automotive.costing.vendorselection.entity.VendorRecommendationDecisionEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VendorRecommendationDecisionEventRepository
        extends JpaRepository<VendorRecommendationDecisionEvent, String> {

    List<VendorRecommendationDecisionEvent> findByRecommendationIdOrderByOccurredAtAsc(String recommendationId);

    List<VendorRecommendationDecisionEvent> findByPublishedFlagFalseOrderByOccurredAtAsc();
}
