package com.automotive.costing.vendorselection.repository;

import com.automotive.costing.vendorselection.entity.VendorRecommendationDecision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VendorRecommendationDecisionRepository
        extends JpaRepository<VendorRecommendationDecision, String> {

    List<VendorRecommendationDecision> findByRecommendationIdOrderByDecidedAtAsc(String recommendationId);
}
