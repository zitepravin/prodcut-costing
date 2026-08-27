package com.automotive.costing.purchase.repository;

import com.automotive.costing.purchase.entity.PurchaseOrderTracking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PurchaseOrderTrackingRepository extends JpaRepository<PurchaseOrderTracking, String> {
    List<PurchaseOrderTracking> findByPurchaseRequestIdOrderByCreatedAtDesc(String purchaseRequestId);

    Optional<PurchaseOrderTracking> findBySourceSystemAndSourceEventId(String sourceSystem, String sourceEventId);
}
