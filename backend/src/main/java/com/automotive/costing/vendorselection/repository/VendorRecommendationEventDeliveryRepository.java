package com.automotive.costing.vendorselection.repository;

import com.automotive.costing.vendorselection.entity.VendorRecommendationEventDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VendorRecommendationEventDeliveryRepository
        extends JpaRepository<VendorRecommendationEventDelivery, String> {

    List<VendorRecommendationEventDelivery> findByEventIdOrderByAttemptedAtDesc(String eventId);

    long countByEventId(String eventId);
}
