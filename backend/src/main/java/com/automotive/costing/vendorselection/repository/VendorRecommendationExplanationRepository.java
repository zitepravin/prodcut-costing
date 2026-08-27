package com.automotive.costing.vendorselection.repository;

import com.automotive.costing.vendorselection.entity.VendorRecommendationExplanation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VendorRecommendationExplanationRepository
        extends JpaRepository<VendorRecommendationExplanation, String> {

    Optional<VendorRecommendationExplanation> findByRecommendationId(String recommendationId);
}
