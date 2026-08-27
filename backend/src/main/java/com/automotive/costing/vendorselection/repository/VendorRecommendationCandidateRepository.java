package com.automotive.costing.vendorselection.repository;

import com.automotive.costing.vendorselection.entity.VendorRecommendationCandidate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VendorRecommendationCandidateRepository extends JpaRepository<VendorRecommendationCandidate, String> {
    List<VendorRecommendationCandidate> findByRecommendationIdOrderByRankPositionAsc(String recommendationId);
}
