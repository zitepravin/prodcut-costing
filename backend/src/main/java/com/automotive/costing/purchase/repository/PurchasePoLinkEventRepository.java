package com.automotive.costing.purchase.repository;

import com.automotive.costing.purchase.entity.PurchasePoLinkEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PurchasePoLinkEventRepository extends JpaRepository<PurchasePoLinkEvent, String> {
    Optional<PurchasePoLinkEvent> findBySourceEventId(String sourceEventId);
    List<PurchasePoLinkEvent> findByPurchaseRequestIdOrderByReceivedAtDesc(String purchaseRequestId);
}
