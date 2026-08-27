package com.automotive.costing.purchase.repository;

import com.automotive.costing.purchase.entity.PurchaseRequestLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseRequestLineRepository extends JpaRepository<PurchaseRequestLine, String> {
    List<PurchaseRequestLine> findByPurchaseRequestIdOrderByLineNoAsc(String purchaseRequestId);
    void deleteByPurchaseRequestId(String purchaseRequestId);
}
